package com.cou.bustracker.service;

import com.cou.bustracker.event.NoticeCreatedEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns a schedule mutation or a new notice into push notifications, off the
 * request thread.
 *
 * <p>Pipeline:
 * <pre>
 *   ScheduleService ──publish──▶ ScheduleChangedEvent
 *   NoticeService   ──publish──▶ NoticeCreatedEvent
 *        │                          │  (transaction commits)
 *        ▼                          ▼
 *   HTTP 200 returned      *ChangedListener (AFTER_COMMIT)
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
 *   <li><b>Audience comes from the data</b>, not from the caller: from the
 *       bus category for schedules, from the notice audience for notices.</li>
 *   <li><b>Localized per device.</b> FCM renders the notification payload in
 *       the tray without the app running, so the text must already be in the
 *       rider's language when we send it. Devices are grouped by the
 *       {@code app_language} they registered with, and each group gets its own
 *       rendered message from the {@code messages_*.properties} bundles.
 *       Notices skip this step on purpose — their title/body <em>are</em>
 *       admin-authored prose, so they are pushed verbatim in whatever
 *       language the admin wrote them.</li>
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

    /**
     * Audiences a notice can target, in fan-out order. Kept as the stored
     * vocabulary (STUDENT / TEACHER / ALL) so the mapping from notice to FCM
     * {@code user_type} groups is a single lookup — {@code ALL} simply walks
     * both rider roles.
     */
    private static final List<String> NOTICE_AUDIENCES = List.of("STUDENT", "TEACHER");

    /**
     * FCM puts a hard-ish cap on payload size and the tray truncates long
     * text anyway, so a runaway body is trimmed before send. The title is
     * already capped at 200 chars by the notices schema.
     */
    private static final int NOTICE_MAX_BODY_CHARS = 500;

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

                sendInBatches(batch, title, body);
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
     * Pushes a freshly created notice to its target audience, off the request
     * thread. Never throws.
     *
     * <p>Unlike schedule pushes, the text is <em>not</em> localized per
     * device: a notice is admin-authored prose (usually Bengali), and the
     * title and body are pushed exactly as written. Only the audience is
     * resolved here — {@code ALL} fans out to both rider roles.
     */
    @Async("notificationExecutor")
    public void dispatch(NoticeCreatedEvent event) {
        try {
            if (event == null) {
                return;
            }

            List<String> audiences = audiencesOf(event.audience());
            if (audiences.isEmpty()) {
                log.warn("Unknown notice audience '{}' — skipping notification for notice {}",
                        event.audience(), event.noticeId());
                return;
            }

            String title = truncate(event.title(), null);
            String body = truncate(event.body(), NOTICE_MAX_BODY_CHARS);

            for (String audience : audiences) {
                Map<String, List<String>> tokensByLanguage = fcmTokenService.getActiveTokensGroupedByLanguage(audience);
                // Notice text is language-independent, so the per-language
                // groups only matter for token routing, not rendering.
                List<String> tokens = tokensByLanguage.values().stream()
                        .flatMap(List::stream)
                        .toList();
                if (tokens.isEmpty()) {
                    log.info("No active {} devices to notify about notice {}", audience, event.noticeId());
                    continue;
                }

                log.info("Pushing notice {} to {} {} device(s): {}",
                        event.noticeId(), tokens.size(), audience, title);
                sendInBatches(tokens, title, body);
            }
        } catch (Exception e) {
            // Deliberately swallowed: the notice write already succeeded and
            // the admin already got their 201. Log loudly instead of failing.
            log.error("Failed to dispatch notice notification {}",
                    event == null ? "?" : event.noticeId(), e);
        }
    }

    /**
     * Maps a stored notice audience onto FCM {@code user_type} groups.
     * A null audience is treated as {@code ALL} (the schema default, but a
     * hand-built event may omit it); anything unrecognized yields an empty
     * list so the caller skips rather than broadcasting to everyone — same
     * rule as an unmapped bus category.
     */
    private static List<String> audiencesOf(String audience) {
        if (audience == null || audience.isBlank()
                || "ALL".equalsIgnoreCase(audience.trim())) {
            return NOTICE_AUDIENCES;
        }
        String normalized = audience.trim().toUpperCase(Locale.ROOT);
        return NOTICE_AUDIENCES.contains(normalized) ? List.of(normalized) : List.of();
    }

    /** Splits tokens into FCM-sized batches and fires one async send per batch. */
    private void sendInBatches(List<String> tokens, String title, String body) {
        for (int from = 0; from < tokens.size(); from += FcmService.MAX_BATCH_SIZE) {
            int to = Math.min(from + FcmService.MAX_BATCH_SIZE, tokens.size());
            fcmService.sendEachAsync(List.copyOf(tokens.subList(from, to)), title, body,
                    fcmTokenService::pruneUnregisteredTokens);
        }
    }

    /**
     * Caps free text for the push payload; {@code maxChars == null} means the
     * schema already bounds the value. An ellipsis marks trimmed text so a
     * rider can tell there is more.
     */
    private static String truncate(String value, Integer maxChars) {
        if (value == null || maxChars == null || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars - 1) + "…";
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
        String busName = localizedBusName(event, locale);
        if (notBlank(busName)) {
            header.append(" · ").append(busName);
        }
        if (notBlank(event.category())) {
            header.append(" · ").append(event.category());
        }
        String route = localizedRouteName(event, locale);
        if (!notBlank(route)) {
            route = notBlank(event.direction()) ? event.direction() : "";
        }
        if (!route.isEmpty()) {
            header.append(" · ").append(route);
        }
        return header.toString();
    }

    /**
     * Bus display name in the device's language, falling back to the other
     * language when only one is set.
     */
    private String localizedBusName(ScheduleChangedEvent event, Locale locale) {
        boolean bangla = "bn".equals(locale.getLanguage());
        String primary = bangla ? event.busNameBn() : event.busNameEn();
        String fallback = bangla ? event.busNameEn() : event.busNameBn();
        return notBlank(primary) ? primary : fallback;
    }

    /**
     * Route name in the device's language, falling back to the other language
     * when only one is set.
     */
    private String localizedRouteName(ScheduleChangedEvent event, Locale locale) {
        boolean bangla = "bn".equals(locale.getLanguage());
        String primary = bangla ? event.routeNameBn() : event.routeNameEn();
        String fallback = bangla ? event.routeNameEn() : event.routeNameBn();
        return notBlank(primary) ? primary : fallback;
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

    private static String fieldKey(Field field) {
        return switch (field) {
            case BUS_NUMBER -> "busNumber";
            case BUS_NAME_BN -> "busNameBn";
            case BUS_NAME_EN -> "busNameEn";
            case DEPARTURE -> "departure";
            case ARRIVAL -> "arrival";
            case DIRECTION -> "direction";
            case ROUTE_NAME_BN -> "routeNameBn";
            case ROUTE_NAME_EN -> "routeNameEn";
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
