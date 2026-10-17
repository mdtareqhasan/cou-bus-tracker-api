package com.cou.bustracker.service;

import com.cou.bustracker.event.ScheduleChangedEvent;
import com.cou.bustracker.event.ScheduleChangedEvent.Field;
import com.cou.bustracker.event.ScheduleChangedEvent.FieldChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns a schedule mutation into push notifications, off the request thread.
 *
 * <p>Pipeline:
 * <pre>
 *   ScheduleService ──publish──▶ ScheduleChangedEvent
 *        │                          │  (transaction commits)
 *        ▼                          ▼
 *   HTTP 200 returned      ScheduleChangedListener (AFTER_COMMIT)
 *                                    │ hand-off to proxy
 *                                    ▼
 *                          NotificationDispatcher#dispatch  ◀── notificationExecutor
 *                                    │ resolve audience ──▶ fetch tokens by language
 *                                    ▼
 *                          render title/body per language (MessageSource)
 *                                    ▼
 *                          FcmService#sendEachAsync  ──▶ FCM (non-blocking)
 *                                    │                    │ callback
 *                                    ▼                    ▼
 *                            pool thread freed     prune dead tokens
 * </pre>
 *
 * <p>Design rules:
 * <ul>
 *   <li><b>Never throws.</b> A push failure must not be able to fail — or
 *       retry — an admin's write.</li>
 *   <li><b>No session.</b> Everything needed is in the immutable event, so
 *       there is no lazy-loading or after-commit surprise.</li>
 *   <li><b>Audience comes from the bus category</b>, not from the caller.</li>
 *   <li><b>Localized per device.</b> FCM renders the notification payload in
 *       the tray without the app running, so the text must already be in the
 *       rider's language when we send it. Devices are grouped by the
 *       {@code app_language} they registered with, and each group gets its own
 *       rendered message from the {@code messages_*.properties} bundles.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    /**
     * Bus category → FCM {@code user_type} audience.
     *
     * <p>COU runs BLUE and STAFF buses interchangeably for the same riders,
     * so both fan out to students; TEACHER buses fan out to teachers. Any
     * category missing here is logged and skipped rather than broadcast to
     * everyone.
     */
    private static final Map<String, String> AUDIENCE_BY_CATEGORY = Map.of(
            "BLUE", "STUDENT",
            "STAFF", "STUDENT",
            "TEACHER", "TEACHER");

    private final FcmTokenService fcmTokenService;
    private final FcmService fcmService;

    /**
     * Message bundles on the classpath ({@code messages.properties} is the
     * English base and fallback; {@code spring.messages.*} in
     * application.yaml disables system-locale fallback so push text never
     * depends on the server's own locale).
     */
    private final MessageSource messageSource;

    /**
     * Runs on {@code notificationExecutor} after the writing transaction has
     * committed. Never propagates an exception.
     */
    @Async("notificationExecutor")
    public void dispatch(ScheduleChangedEvent event) {
        try {
            if (event == null) {
                return;
            }
            if (!shouldNotify(event)) {
                log.info("Skipping push for inactive schedule {} ({})",
                        event.scheduleId(), event.action());
                return;
            }

            String audience = AUDIENCE_BY_CATEGORY.get(event.category());
            if (audience == null) {
                log.warn("No push audience mapped for bus category '{}' — skipping notification for schedule {}",
                        event.category(), event.scheduleId());
                return;
            }

            Map<String, List<String>> tokensByLanguage = fcmTokenService.getActiveTokensGroupedByLanguage(audience);
            int deviceCount = tokensByLanguage.values().stream().mapToInt(List::size).sum();
            if (deviceCount == 0) {
                log.info("No active {} devices to notify about schedule {} ({})",
                        audience, event.scheduleId(), event.action());
                return;
            }

            // One rendered message per language, one FCM fan-out per group.
            for (Map.Entry<String, List<String>> group : tokensByLanguage.entrySet()) {
                Locale locale = localeOf(group.getKey());
                List<String> batch = group.getValue();
                String title = title(event, locale);
                String body = body(event, locale);
                log.info("Pushing schedule {} ({}) to {} {} device(s) in {}: {}",
                        event.scheduleId(), event.action(), batch.size(), audience, locale, title);

                for (int from = 0; from < batch.size(); from += FcmService.MAX_BATCH_SIZE) {
                    int to = Math.min(from + FcmService.MAX_BATCH_SIZE, batch.size());
                    fcmService.sendEachAsync(List.copyOf(batch.subList(from, to)), title, body,
                            fcmTokenService::pruneUnregisteredTokens);
                }
            }
        } catch (Exception e) {
            // Deliberately swallowed: the schedule write already succeeded and
            // the admin already got their 200. Log loudly instead of failing.
            log.error("Failed to dispatch schedule notification {} ({})",
                    event == null ? "?" : event.scheduleId(),
                    event == null ? "?" : event.action(), e);
        }
    }

    /**
     * Only schedules a rider can actually see are worth announcing — the public
     * endpoints list active schedules only, so editing a dormant one is admin
     * bookkeeping, not news. Toggles always notify, because that is exactly the
     * moment a service starts or stops.
     */
    private static boolean shouldNotify(ScheduleChangedEvent event) {
        return switch (event.action()) {
            case ACTIVATED, DEACTIVATED -> true;
            case CREATED, UPDATED, DELETED -> event.scheduleActive();
        };
    }

    private String title(ScheduleChangedEvent event, Locale locale) {
        String bus = event.busNumber() != null ? event.busNumber() : msg(locale, "push.bus.fallback");
        return switch (event.action()) {
            case CREATED -> msg(locale, "push.schedule.created.title", bus);
            case UPDATED -> msg(locale, "push.schedule.updated.title", bus);
            case DELETED -> msg(locale, "push.schedule.deleted.title", bus);
            case ACTIVATED -> msg(locale, "push.schedule.activated.title", bus);
            case DEACTIVATED -> msg(locale, "push.schedule.deactivated.title", bus);
        };
    }

    private String body(ScheduleChangedEvent event, Locale locale) {
        String header = header(event, locale);

        String detail = switch (event.action()) {
            // when() already says exactly when it runs, e.g.
            // "departs 07:00–08:00 (SUN-THU)"
            case CREATED -> msg(locale, "push.body.created", when(event, locale));
            // changes() says exactly which fields moved, e.g.
            // "departure: 07:00 → 07:15" — each label localized per field.
            case UPDATED -> String.join(", ", event.changes().stream()
                    .map(change -> describe(change, locale)).toList());
            case DELETED -> msg(locale, "push.body.deleted", when(event, locale));
            case ACTIVATED -> msg(locale, "push.body.activated", when(event, locale));
            case DEACTIVATED -> msg(locale, "push.body.deactivated", when(event, locale));
        };

        return header + " · " + detail;
    }

    /** "BUS 03 · COU Blue · BLUE · Kandirpar → Campus" */
    private String header(ScheduleChangedEvent event, Locale locale) {
        StringBuilder header = new StringBuilder(
                event.busNumber() != null ? event.busNumber() : msg(locale, "push.bus.fallback"));
        if (notBlank(event.busName())) {
            header.append(" · ").append(event.busName());
        }
        if (notBlank(event.category())) {
            header.append(" · ").append(event.category());
        }
        String route = route(event);
        if (!route.isEmpty()) {
            header.append(" · ").append(route);
        }
        return header.toString();
    }

    /** "departs 07:00–08:00 (SUN-THU)" */
    private String when(ScheduleChangedEvent event, Locale locale) {
        StringBuilder when = event.arrivalTime() != null
                ? new StringBuilder(msg(locale, "push.when.range",
                format(event.departureTime()), format(event.arrivalTime())))
                : new StringBuilder(msg(locale, "push.when.single", format(event.departureTime())));
        if (notBlank(event.days())) {
            when.append(" (").append(event.days()).append(")");
        }
        return when.toString();
    }

    /** "departure: 07:00 → 07:15" — label localized, values are raw data. */
    private String describe(FieldChange change, Locale locale) {
        return msg(locale, "push.change",
                msg(locale, "push.field." + fieldKey(change.field())),
                display(change.from(), locale),
                display(change.to(), locale));
    }

    /** Renders a diff value, replacing "no value" with a localized placeholder. */
    private String display(String value, Locale locale) {
        return notBlank(value) ? value : msg(locale, "push.value.none");
    }

    private String route(ScheduleChangedEvent event) {
        if (notBlank(event.startPoint()) && notBlank(event.endPoint())) {
            return event.startPoint() + " → " + event.endPoint();
        }
        return notBlank(event.direction()) ? event.direction() : "";
    }

    private static String fieldKey(Field field) {
        return switch (field) {
            case BUS_NUMBER -> "busNumber";
            case BUS_NAME -> "busName";
            case DEPARTURE -> "departure";
            case ARRIVAL -> "arrival";
            case DIRECTION -> "direction";
            case START_POINT -> "start";
            case END_POINT -> "end";
            case DAYS -> "days";
        };
    }

    private static String format(LocalTime time) {
        return time == null ? "?" : String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    /**
     * Resolves a stored language code ("bn" / "en") to a locale. Anything
     * unparseable becomes the base bundle (English) rather than throwing.
     */
    private static Locale localeOf(String language) {
        try {
            return Locale.forLanguageTag(language);
        } catch (Exception e) {
            return Locale.ENGLISH;
        }
    }

    /**
     * Resolves one message with {@code MessageFormat} arguments. A missing key
     * degrades to the key itself — a half-translated push is still better than
     * a swallowed exception losing the whole notification (or, worse, the
     * never-throws rule breaking).
     */
    private String msg(Locale locale, String key, Object... args) {
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Missing push message key '{}' for locale {}", key, locale);
            return key;
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
