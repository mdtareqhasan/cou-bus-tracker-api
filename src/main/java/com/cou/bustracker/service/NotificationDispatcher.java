package com.cou.bustracker.service;

import com.cou.bustracker.event.ScheduleChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
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
 *                                    │ resolve audience ──▶ fetch tokens
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

            List<String> tokens = fcmTokenService.getActiveTokensForRole(audience);
            if (tokens.isEmpty()) {
                log.info("No active {} devices to notify about schedule {} ({})",
                        audience, event.scheduleId(), event.action());
                return;
            }

            String title = title(event);
            String body = body(event);
            log.info("Pushing schedule {} ({}) to {} {} device(s): {}",
                    event.scheduleId(), event.action(), tokens.size(), audience, title);

            for (int from = 0; from < tokens.size(); from += FcmService.MAX_BATCH_SIZE) {
                int to = Math.min(from + FcmService.MAX_BATCH_SIZE, tokens.size());
                List<String> batch = List.copyOf(tokens.subList(from, to));
                fcmService.sendEachAsync(batch, title, body,
                        fcmTokenService::pruneUnregisteredTokens);
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

    private static String title(ScheduleChangedEvent event) {
        String bus = event.busNumber() != null ? event.busNumber() : "Bus";
        return switch (event.action()) {
            case CREATED -> "New schedule: " + bus;
            case UPDATED -> "Schedule updated: " + bus;
            case DELETED -> "Schedule removed: " + bus;
            case ACTIVATED -> "Service resumed: " + bus;
            case DEACTIVATED -> "Service paused: " + bus;
        };
    }

    private static String body(ScheduleChangedEvent event) {
        StringBuilder body = new StringBuilder(header(event)).append(" · ");

        switch (event.action()) {
            case CREATED -> body.append(when(event)).append(" added");
            // changes() already says exactly which fields moved, e.g.
            // "departure 07:00 → 07:15" or "bus name: Blue → COU Blue"
            case UPDATED -> body.append(String.join(", ", event.changes()));
            case DELETED -> body.append(when(event)).append(" no longer runs");
            case ACTIVATED -> body.append(when(event)).append(" is running again");
            case DEACTIVATED -> body.append(when(event)).append(" is paused for now");
        }

        return body.toString();
    }

    /** "BUS 03 · COU Blue · BLUE · Kandirpar → Campus" */
    private static String header(ScheduleChangedEvent event) {
        StringBuilder header = new StringBuilder(event.busNumber() != null ? event.busNumber() : "Bus");
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
    private static String when(ScheduleChangedEvent event) {
        StringBuilder when = new StringBuilder("departs ").append(format(event.departureTime()));
        if (event.arrivalTime() != null) {
            when.append("–").append(format(event.arrivalTime()));
        }
        if (notBlank(event.days())) {
            when.append(" (").append(event.days()).append(")");
        }
        return when.toString();
    }

    private static String route(ScheduleChangedEvent event) {
        if (notBlank(event.startPoint()) && notBlank(event.endPoint())) {
            return event.startPoint() + " → " + event.endPoint();
        }
        return notBlank(event.direction()) ? event.direction() : "";
    }

    private static String format(LocalTime time) {
        return time == null ? "?" : String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
