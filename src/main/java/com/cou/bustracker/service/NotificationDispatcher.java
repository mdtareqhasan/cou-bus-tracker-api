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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
 *       there is no lazy-loading or after-commit surprise. Route <em>names</em>
 *       ride the event; only the audience (who follows which route) is read
 *       here, from the preference table.</li>
 *   <li><b>Audience comes from the data</b>, not from the caller: for
 *       schedules, the riders who opted in to that route's alerts via
 *       {@code user_route_preferences}; for notices, the notice's audience.
 *       A schedule change is only news to people who follow that route, so it
 *       never fans out to an entire user type.</li>
 *   <li><b>Reassignments fan out twice.</b> When an update moves a trip from
 *       one route to another, riders of the old route hear "no longer runs on
 *       your route" and riders of the new route hear "new trip" — each side
 *       gets the wording that is true <em>for them</em>. Someone following
 *       both routes hears only the "new trip" half.</li>
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
    private final UserRoutePreferenceService preferenceService;

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
            if (event.routeId() == null) {
                log.warn("Schedule {} ({}) has no route — no audience to notify",
                        event.scheduleId(), event.action());
                return;
            }

            Long newRouteId = event.routeId();
            Long oldRouteId = event.previousRouteId();
            boolean reassigned = oldRouteId != null && !oldRouteId.equals(newRouteId);

            if (!reassigned) {
                // One route, one audience: everyone who asked for alerts on it.
                pushToRouteSubscribers(event, newRouteId, Flavor.of(event.action()), null);
                return;
            }

            // A reassignment is news on both sides of the move. Riders of the
            // old route hear that the trip is gone from their route; riders of
            // the new route hear the trip joined theirs. Someone following both
            // gets only the second message — one push per change per device.
            Map<String, Set<Long>> newAudience = preferenceService.alertSubscribersByType(newRouteId);
            Map<String, Set<Long>> oldOnly = minus(preferenceService.alertSubscribersByType(oldRouteId),
                    newAudience);

            pushToRouteSubscribers(event, oldRouteId, Flavor.LEFT, oldOnly);
            pushToRouteSubscribers(event, newRouteId, Flavor.JOINED, newAudience);
        } catch (Exception e) {
            // Deliberately swallowed: the schedule write already succeeded and
            // the admin already got their 200. Log loudly instead of failing.
            log.error("Failed to dispatch schedule notification {} ({})",
                    event == null ? "?" : event.scheduleId(),
                    event == null ? "?" : event.action(), e);
        }
    }

    /**
     * Renders and pushes one message to a route's alert subscribers.
     *
     * @param audience pre-resolved (user_type → user ids); pass null to look
     *                 up subscribers of {@code routeId} yourself
     */
    private void pushToRouteSubscribers(ScheduleChangedEvent event, Long routeId, Flavor flavor,
                                        Map<String, Set<Long>> audience) {
        Map<String, Set<Long>> subscribers =
                audience != null ? audience : preferenceService.alertSubscribersByType(routeId);

        int deviceCount = 0;
        for (Map.Entry<String, Set<Long>> group : subscribers.entrySet()) {
            Map<String, List<String>> tokensByLanguage =
                    fcmTokenService.getActiveTokensGroupedByLanguage(group.getKey(), group.getValue());
            for (Map.Entry<String, List<String>> devices : tokensByLanguage.entrySet()) {
                Locale locale = localeOf(devices.getKey());
                List<String> batch = devices.getValue();
                String title = title(event, locale, flavor);
                String body = body(event, locale, flavor);
                log.info("Pushing schedule {} ({}, route {}) to {} {} device(s) in {}: {}",
                        event.scheduleId(), event.action(), routeId, batch.size(), group.getKey(),
                        locale, title);
                sendInBatches(batch, title, body, routeLink(routeId));
                deviceCount += batch.size();
            }
        }
        if (deviceCount == 0) {
            log.info("No subscribed devices to notify about schedule {} ({}) on route {}",
                    event.scheduleId(), event.action(), routeId);
        }
    }

    /**
     * {@code from} minus {@code remove}, per user type — the old-route-only
     * audience of a reassignment. A rider following both routes already hears
     * the "joined" message, so the "left" message would be noise.
     */
    private static Map<String, Set<Long>> minus(Map<String, Set<Long>> from, Map<String, Set<Long>> remove) {
        Map<String, Set<Long>> out = new LinkedHashMap<>();
        from.forEach((type, ids) -> {
            Set<Long> kept = new LinkedHashSet<>(ids);
            remove.getOrDefault(type, Set.of()).forEach(kept::remove);
            if (!kept.isEmpty()) {
                out.put(type, kept);
            }
        });
        return out;
    }

    /**
     * Which wording a schedule push uses. Ordinals match the action enum for
     * the common case, so {@link Flavor#of} falls through to the action itself
     * except for the two reassignment halves.
     */
    private enum Flavor {
        /** Normal send — title/body follow the event's own action. */
        STANDARD,
        /** Trip moved off the rider's route: "no longer runs (was departing …)". */
        LEFT,
        /** Trip moved onto the rider's route: "new trip departs …". */
        JOINED;

        static Flavor of(ScheduleChangedEvent.Action action) {
            return switch (action) {
                case CREATED -> JOINED;   // a new trip arriving on a route reads the same
                default -> STANDARD;
            };
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
                sendInBatches(tokens, title, body, "/notices");
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
    private void sendInBatches(List<String> tokens, String title, String body, String route) {
        for (int from = 0; from < tokens.size(); from += FcmService.MAX_BATCH_SIZE) {
            int to = Math.min(from + FcmService.MAX_BATCH_SIZE, tokens.size());
            fcmService.sendEachAsync(List.copyOf(tokens.subList(from, to)), title, body, route,
                    fcmTokenService::pruneUnregisteredTokens);
        }
    }

    /**
     * In-app destination for a schedule push: the Route tab's detail page for
     * the direction the message is about. For a "left your route" message that
     * is the <em>old</em> route — the one the rider follows — so tapping the
     * push lands them where the news applies. The Flutter side registers
     * {@code /route/:id}; an unknown id still opens the Route tab, so a stale
     * link degrades instead of breaking.
     */
    private static String routeLink(Long routeId) {
        return routeId == null ? null : "/route/" + routeId;
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

    private String title(ScheduleChangedEvent event, Locale locale, Flavor flavor) {
        String bus = event.busNumber() != null ? event.busNumber() : msg(locale, "push.bus.fallback");
        // The two halves of a reassignment reuse the delete/create titles: to a
        // rider of the old route the trip really is gone, and to a rider of the
        // new route it really is new.
        return switch (flavor) {
            case LEFT -> msg(locale, "push.schedule.deleted.title", bus);
            case JOINED -> msg(locale, "push.schedule.created.title", bus);
            case STANDARD -> switch (event.action()) {
                case CREATED -> msg(locale, "push.schedule.created.title", bus);
                case UPDATED -> msg(locale, "push.schedule.updated.title", bus);
                case DELETED -> msg(locale, "push.schedule.deleted.title", bus);
                case ACTIVATED -> msg(locale, "push.schedule.activated.title", bus);
                case DEACTIVATED -> msg(locale, "push.schedule.deactivated.title", bus);
            };
        };
    }

    private String body(ScheduleChangedEvent event, Locale locale, Flavor flavor) {
        String detail = switch (flavor) {
            // Header names the route the rider follows — the one the trip
            // left — so the news reads as being about *their* route.
            case LEFT -> msg(locale, "push.body.deleted", wasBefore(event, locale));
            case JOINED -> msg(locale, "push.body.created", when(event, locale));
            case STANDARD -> switch (event.action()) {
                // when() says exactly when it runs, e.g. "departs 7:00 AM (SUN-THU)";
                // the body lead-in turns it into a full sentence.
                case CREATED -> msg(locale, "push.body.created", when(event, locale));
                // One plain-language sentence per changed field, in the order a
                // rider cares most about (times, then route/days, then names).
                case UPDATED -> sentences(event, locale);
                case DELETED -> msg(locale, "push.body.deleted", was(event, locale));
                case ACTIVATED -> msg(locale, "push.body.activated", when(event, locale));
                case DEACTIVATED -> msg(locale, "push.body.deactivated", was(event, locale));
            };
        };

        return header(event, locale, flavor == Flavor.LEFT) + " · " + detail;
    }

    /** "BUS 03 · COU Blue · BLUE · Kandirpar → Campus" */
    private String header(ScheduleChangedEvent event, Locale locale, boolean previousRoute) {
        StringBuilder header = new StringBuilder(
                event.busNumber() != null ? event.busNumber() : msg(locale, "push.bus.fallback"));
        String busName = localizedBusName(event, locale);
        if (notBlank(busName)) {
            header.append(" · ").append(busName);
        }
        if (notBlank(event.category())) {
            header.append(" · ").append(event.category());
        }
        String route = previousRoute
                ? localizedRouteName(event.previousRouteNameBn(), event.previousRouteNameEn(), locale)
                : localizedRouteName(event.routeNameBn(), event.routeNameEn(), locale);
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
    private static String localizedRouteName(String nameBn, String nameEn, Locale locale) {
        boolean bangla = "bn".equals(locale.getLanguage());
        String primary = bangla ? nameBn : nameEn;
        String fallback = bangla ? nameEn : nameBn;
        return notBlank(primary) ? primary : fallback;
    }

    /** "departs 7:00 AM (SUN-THU)" — arrival time is never announced. */
    private String when(ScheduleChangedEvent event, Locale locale) {
        StringBuilder when = new StringBuilder(msg(locale, "push.when.single", format(event.departureTime())));
        if (notBlank(event.days())) {
            when.append(" (").append(event.days()).append(")");
        }
        return when.toString();
    }

    /** "was departing 7:00 AM (SUN-THU)" — for delete/deactivate, past tense. */
    private String was(ScheduleChangedEvent event, Locale locale) {
        StringBuilder was = new StringBuilder(msg(locale, "push.when.was.single", format(event.departureTime())));
        if (notBlank(event.days())) {
            was.append(" (").append(event.days()).append(")");
        }
        return was.toString();
    }

    /**
     * Past tense for a "left your route" message: the departure and days the
     * trip had <em>before</em> this change. The event carries the schedule's
     * current state, but a reassignment may have moved the time too — quoting
     * the new time as what the rider "was" getting would be wrong, so the old
     * value is read back out of the diff. When a field didn't move, its old
     * value is simply the current one.
     */
    private String wasBefore(ScheduleChangedEvent event, Locale locale) {
        FieldChange departure = changeOf(event.changes(), Field.DEPARTURE);
        // Diff times are raw "HH:mm" — re-render to 12-hour like the rest.
        String oldDeparture = departure != null && notBlank(departure.from())
                ? clock(departure.from())
                : format(event.departureTime());
        FieldChange days = changeOf(event.changes(), Field.DAYS);
        String oldDays = days != null && notBlank(days.from()) ? days.from() : event.days();

        StringBuilder was = new StringBuilder(msg(locale, "push.when.was.single", oldDeparture));
        if (notBlank(oldDays)) {
            was.append(" (").append(oldDays).append(")");
        }
        return was.toString();
    }

    /**
     * Renders an update as one plain-language sentence per changed field, joined
     * by " · " in the order a rider cares most about (times → route → days →
     * bus → direction). The bilingual route/bus name fields collapse into a
     * single human sentence each, since a rider never thinks of the route as a
     * "Bangla" vs "English" change — only that the route text moved.
     */
    private String sentences(ScheduleChangedEvent event, Locale locale) {
        List<FieldChange> changes = event.changes();
        List<Field> present = changes.stream().map(FieldChange::field).toList();

        List<String> out = new ArrayList<>();
        if (present.contains(Field.DEPARTURE)) {
            out.add(timeSentence(Field.DEPARTURE, "push.change.departureFrom", "push.change.departureTo", changes, locale));
        }
        // Arrival time is never announced, and an arrival-only edit produces no
        // change list at all (ScheduleService doesn't diff it), so there is no
        // ARRIVAL sentence here by design.
        String route = routeSentence(changes, locale);
        if (route != null) {
            out.add(route);
        }
        if (present.contains(Field.DAYS)) {
            out.add(msg(locale, "push.change.days", valueOf(Field.DAYS, changes, locale)));
        }
        String busName = busNameSentence(changes, locale);
        if (busName != null) {
            out.add(busName);
        }
        if (present.contains(Field.BUS_NUMBER)) {
            out.add(msg(locale, "push.change.busNumber", valueOf(Field.BUS_NUMBER, changes, locale)));
        }
        if (present.contains(Field.DIRECTION)) {
            out.add(msg(locale, "push.change.direction", valueOf(Field.DIRECTION, changes, locale)));
        }
        return String.join(" ", out);
    }

    /**
     * A time change sentence that names the previous value only when there was
     * one — "Now departs at 7:15 AM (was 7:00 AM)" vs a clean "Now departs at
     * 7:15 AM" for a freshly added time. Times are raw "HH:mm" from the diff and
     * are re-rendered to 12-hour here so the sentence matches the rest of the
     * message.
     */
    private String timeSentence(Field field, String fromKey, String toKey, List<FieldChange> changes, Locale locale) {
        FieldChange change = changeOf(changes, field);
        String to = clock(change.to());
        if (!notBlank(change.from())) {
            return msg(locale, toKey, to);
        }
        return msg(locale, fromKey, to, clock(change.from()));
    }

    /**
     * The route sentence uses whichever language the change carried; a bilingual
     * edit that moved both resolves to the device's preferred language.
     */
    private String routeSentence(List<FieldChange> changes, Locale locale) {
        FieldChange bn = changeOf(changes, Field.ROUTE_NAME_BN);
        FieldChange en = changeOf(changes, Field.ROUTE_NAME_EN);
        if (bn == null && en == null) {
            return null;
        }
        boolean bangla = "bn".equals(locale.getLanguage());
        FieldChange primary = bangla ? (bn != null ? bn : en) : (en != null ? en : bn);
        return msg(locale, "push.change.route", display(primary.to(), locale));
    }

    /**
     * Bus-name edits also collapse the Bangla/English fields into one sentence
     * naming the device's preferred language.
     */
    private String busNameSentence(List<FieldChange> changes, Locale locale) {
        FieldChange bn = changeOf(changes, Field.BUS_NAME_BN);
        FieldChange en = changeOf(changes, Field.BUS_NAME_EN);
        if (bn == null && en == null) {
            return null;
        }
        boolean bangla = "bn".equals(locale.getLanguage());
        FieldChange primary = bangla ? (bn != null ? bn : en) : (en != null ? en : bn);
        return msg(locale, "push.change.busName", display(primary.to(), locale));
    }

    private static FieldChange changeOf(List<FieldChange> changes, Field field) {
        return changes.stream().filter(c -> c.field() == field).findFirst().orElse(null);
    }

    /** The "to" value of the first change touching {@code field}, or a placeholder. */
    private String valueOf(Field field, List<FieldChange> changes, Locale locale) {
        FieldChange change = changeOf(changes, field);
        return change == null ? msg(locale, "push.value.none") : display(change.to(), locale);
    }

    /** Renders a diff value, replacing "no value" with a localized placeholder. */
    private String display(String value, Locale locale) {
        return notBlank(value) ? value : msg(locale, "push.value.none");
    }

    /** Re-renders a raw "HH:mm" diff time as 12-hour AM/PM ("7:15 AM"). */
    private static String clock(String hhmm) {
        if (!notBlank(hhmm)) {
            return "?";
        }
        String[] parts = hhmm.trim().split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        String meridiem = hour < 12 ? "AM" : "PM";
        int h12 = hour % 12;
        if (h12 == 0) {
            h12 = 12;
        }
        return String.format("%d:%02d %s", h12, minute, meridiem);
    }

    private static String format(LocalTime time) {
        if (time == null) {
            return "?";
        }
        int hour = time.getHour();
        String meridiem = hour < 12 ? "AM" : "PM";
        int h12 = hour % 12;
        if (h12 == 0) {
            h12 = 12;
        }
        return String.format("%d:%02d %s", h12, time.getMinute(), meridiem);
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
