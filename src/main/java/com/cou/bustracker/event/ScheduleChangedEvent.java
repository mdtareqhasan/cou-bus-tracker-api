package com.cou.bustracker.event;

import com.cou.bustracker.entity.Bus;
import com.cou.bustracker.entity.Route;
import com.cou.bustracker.entity.Schedule;

import java.time.LocalTime;
import java.util.List;

/**
 * Immutable snapshot of a schedule mutation, published after the DB commit.
 *
 * <p>Everything a push message needs is copied out of the entity <em>while the
 * transaction is still open</em>. That matters twice over:
 * <ul>
 *   <li>{@code Schedule.bus} and {@code Schedule.route} are lazy — reading them
 *       after commit would throw {@code LazyInitializationException}.</li>
 *   <li>The listener fires AFTER_COMMIT, so for a DELETE the row (and any
 *       entity state) may already be gone.</li>
 * </ul>
 *
 * <p>A plain record also means the payload can be handed to another thread
 * with no session, no proxy and no risk of dirty-checking surprises.
 *
 * @param action         what happened to the schedule
 * @param scheduleId     id of the schedule
 * @param busId          id of the bus it belongs to
 * @param busNumber      e.g. "BUS 03"
 * @param busNameBn      optional Bangla display name, may be null
 * @param busNameEn      optional English display name, may be null
 * @param category       bus category — BLUE / TEACHER / STAFF; decides the
 *                       notification audience
 * @param direction      UP / DOWN
 * @param routeId        id of the route this trip runs (routes belong to the
 *                       schedule — a bus serves different routes at different
 *                       times of day)
 * @param routeNameBn    Bangla route name, may be null
 * @param routeNameEn    English route name, may be null
 * @param previousRouteId       the route this trip ran before the update, or
 *                              null when it didn't move (create/delete/toggle)
 * @param previousRouteNameBn   Bangla name of the previous route, may be null
 * @param previousRouteNameEn   English name of the previous route, may be null
 * @param departureTime  required by the schema
 * @param arrivalTime    optional
 * @param days           e.g. "SUN-THU"
 * @param scheduleActive whether the schedule was active at snapshot time
 * @param changes        structured field diffs (update only, empty otherwise)
 *                       so the message says what actually moved. Structured
 *                       rather than pre-rendered English so the dispatcher can
 *                       localize each field label per device language.
 */
public record ScheduleChangedEvent(
        Action action,
        Long scheduleId,
        Long busId,
        String busNumber,
        String busNameBn,
        String busNameEn,
        String category,
        String direction,
        Long routeId,
        String routeNameBn,
        String routeNameEn,
        Long previousRouteId,
        String previousRouteNameBn,
        String previousRouteNameEn,
        LocalTime departureTime,
        LocalTime arrivalTime,
        String days,
        boolean scheduleActive,
        List<FieldChange> changes) {

    public enum Action {
        CREATED, UPDATED, DELETED, ACTIVATED, DEACTIVATED
    }

    /**
     * One field that moved in an update.
     *
     * <p>Values are raw display data, not sentences: null means "had/will have
     * no value" and is rendered as a localized placeholder at push time. Field
     * labels are deliberately <em>not</em> carried here — they come from the
     * message bundles keyed by {@link Field}, so the same diff renders in any
     * language.
     */
    public record FieldChange(Field field, String from, String to) {
    }

    /**
     * Schedule fields a rider can see move in an update. Arrival time is
     * deliberately absent: it never appears in a push, and an arrival-only edit
     * is not worth a notification at all.
     */
    public enum Field {
        BUS_NUMBER, BUS_NAME_BN, BUS_NAME_EN, DEPARTURE, DIRECTION,
        ROUTE_NAME_BN, ROUTE_NAME_EN, DAYS
    }

    public ScheduleChangedEvent {
        changes = changes == null ? List.of() : List.copyOf(changes);
    }

    /**
     * Snapshots a live entity. <strong>Must be called inside the transaction</strong>
     * that owns {@code schedule} — never from an after-commit listener.
     */
    public static ScheduleChangedEvent from(Action action, Schedule schedule, List<FieldChange> changes) {
        return from(action, schedule, changes, null, null, null);
    }

    /**
     * As {@link #from(Action, Schedule, List)}, but carrying the route the trip
     * ran <em>before</em> this update. {@code ScheduleService} captures those
     * before the mutation and passes them here, so the dispatcher can fan a
     * reassignment out to both routes' subscribers with each side's own wording
     * — an after-commit listener could no longer read the old route.
     */
    public static ScheduleChangedEvent from(Action action, Schedule schedule, List<FieldChange> changes,
                                            Long previousRouteId,
                                            String previousRouteNameBn, String previousRouteNameEn) {
        Bus bus = schedule.getBus();
        Route route = schedule.getRoute();
        return new ScheduleChangedEvent(
                action,
                schedule.getId(),
                bus.getId(),
                bus.getBusNumber(),
                bus.getBusNameBn(),
                bus.getBusNameEn(),
                bus.getCategory(),
                schedule.getDirection(),
                route != null ? route.getId() : null,
                route != null ? route.getNameBn() : null,
                route != null ? route.getNameEn() : null,
                previousRouteId,
                previousRouteNameBn,
                previousRouteNameEn,
                schedule.getDepartureTime(),
                schedule.getArrivalTime(),
                schedule.getDays(),
                Boolean.TRUE.equals(schedule.getIsActive()),
                changes);
    }
}
