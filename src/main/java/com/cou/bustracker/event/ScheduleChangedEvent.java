package com.cou.bustracker.event;

import com.cou.bustracker.entity.Bus;
import com.cou.bustracker.entity.Schedule;

import java.time.LocalTime;
import java.util.List;

/**
 * Immutable snapshot of a schedule mutation, published after the DB commit.
 *
 * <p>Everything a push message needs is copied out of the entity <em>while the
 * transaction is still open</em>. That matters twice over:
 * <ul>
 *   <li>{@code Schedule.bus} is lazy — reading it after commit would throw
 *       {@code LazyInitializationException}.</li>
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
 * @param busName        optional display name, may be null
 * @param category       bus category — BLUE / TEACHER / STAFF; decides the
 *                       notification audience
 * @param direction      UP / DOWN
 * @param startPoint     optional
 * @param endPoint       optional
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
        String busName,
        String category,
        String direction,
        String startPoint,
        String endPoint,
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

    /** Schedule fields a rider can see move in an update. */
    public enum Field {
        BUS_NUMBER, BUS_NAME, DEPARTURE, ARRIVAL, DIRECTION, START_POINT, END_POINT, DAYS
    }

    public ScheduleChangedEvent {
        changes = changes == null ? List.of() : List.copyOf(changes);
    }

    /**
     * Snapshots a live entity. <strong>Must be called inside the transaction</strong>
     * that owns {@code schedule} — never from an after-commit listener.
     */
    public static ScheduleChangedEvent from(Action action, Schedule schedule, List<FieldChange> changes) {
        Bus bus = schedule.getBus();
        return new ScheduleChangedEvent(
                action,
                schedule.getId(),
                bus.getId(),
                bus.getBusNumber(),
                bus.getBusName(),
                bus.getCategory(),
                schedule.getDirection(),
                schedule.getStartPoint(),
                schedule.getEndPoint(),
                schedule.getDepartureTime(),
                schedule.getArrivalTime(),
                schedule.getDays(),
                Boolean.TRUE.equals(schedule.getIsActive()),
                changes);
    }
}
