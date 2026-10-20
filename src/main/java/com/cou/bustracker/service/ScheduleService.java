package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.CreateScheduleRequest;
import com.cou.bustracker.dto.response.ScheduleResponse;
import com.cou.bustracker.entity.Bus;
import com.cou.bustracker.entity.Route;
import com.cou.bustracker.entity.Schedule;
import com.cou.bustracker.event.ScheduleChangedEvent;
import com.cou.bustracker.event.ScheduleChangedEvent.Action;
import com.cou.bustracker.event.ScheduleChangedEvent.Field;
import com.cou.bustracker.event.ScheduleChangedEvent.FieldChange;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.BusRepository;
import com.cou.bustracker.repository.RouteRepository;
import com.cou.bustracker.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<ScheduleResponse> getAllActiveSchedules() {
        List<Schedule> schedules = scheduleRepository.findAllWithBus();
        return schedules.stream()
                .map(this::mapToScheduleResponse)
                .collect(Collectors.toList());
    }

    public List<ScheduleResponse> getSchedulesByBusId(Long busId) {
        List<Schedule> schedules = scheduleRepository.findByBusIdWithBus(busId);
        return schedules.stream()
                .map(this::mapToScheduleResponse)
                .collect(Collectors.toList());
    }

    public List<ScheduleResponse> getAllSchedulesForAdmin() {
        List<Schedule> schedules = scheduleRepository.findAllWithBusForAdmin();
        return schedules.stream()
                .map(this::mapToScheduleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void toggleSchedule(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
        schedule.setIsActive(!schedule.getIsActive());
        scheduleRepository.save(schedule);

        boolean nowActive = Boolean.TRUE.equals(schedule.getIsActive());
        eventPublisher.publishEvent(ScheduleChangedEvent.from(
                nowActive ? Action.ACTIVATED : Action.DEACTIVATED, schedule, List.of()));
    }

    public Schedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
    }

    @Transactional
    public void addSchedule(CreateScheduleRequest request) {
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + request.getBusId()));
        if (request.getBusNameBn() != null && !request.getBusNameBn().isBlank()) {
            bus.setBusNameBn(request.getBusNameBn());
        }
        if (request.getBusNameEn() != null && !request.getBusNameEn().isBlank()) {
            bus.setBusNameEn(request.getBusNameEn());
        }

        Schedule schedule = Schedule.builder()
                .bus(bus)
                .route(findRoute(request.getRouteId()))
                .departureTime(LocalTime.parse(request.getDepartureTime()))
                .arrivalTime(request.getArrivalTime() != null ? LocalTime.parse(request.getArrivalTime()) : null)
                .direction(request.getDirection())
                .days(request.getDays() != null ? request.getDays() : "SUN-THU")
                .isActive(true)
                .build();

        Schedule saved = scheduleRepository.save(schedule);
        eventPublisher.publishEvent(ScheduleChangedEvent.from(Action.CREATED, saved, List.of()));
    }

    @Transactional
    public void updateSchedule(Long id, CreateScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + request.getBusId()));

        // Snapshot everything first: the bus names are mutated below, and when
        // the request points at the same bus, `bus` IS schedule.getBus() (same
        // persistence-context instance), so reading afterwards would hide it.
        final Long previousBusId = schedule.getBus().getId();
        final String previousBusNumber = schedule.getBus().getBusNumber();
        final String previousBusNameBn = bus.getBusNameBn();
        final String previousBusNameEn = bus.getBusNameEn();
        final LocalTime previousDeparture = schedule.getDepartureTime();
        final LocalTime previousArrival = schedule.getArrivalTime();
        final String previousDirection = schedule.getDirection();
        final String previousRouteNameBn = schedule.getRoute() != null ? schedule.getRoute().getNameBn() : null;
        final String previousRouteNameEn = schedule.getRoute() != null ? schedule.getRoute().getNameEn() : null;
        final String previousDays = schedule.getDays();

        if (request.getBusNameBn() != null && !request.getBusNameBn().isBlank()) {
            bus.setBusNameBn(request.getBusNameBn());
        }
        if (request.getBusNameEn() != null && !request.getBusNameEn().isBlank()) {
            bus.setBusNameEn(request.getBusNameEn());
        }

        schedule.setBus(bus);
        schedule.setRoute(findRoute(request.getRouteId()));
        schedule.setDepartureTime(LocalTime.parse(request.getDepartureTime()));
        schedule.setArrivalTime(request.getArrivalTime() != null && !request.getArrivalTime().isBlank()
                ? LocalTime.parse(request.getArrivalTime()) : null);
        schedule.setDirection(request.getDirection());
        schedule.setDays(request.getDays() != null ? request.getDays() : "SUN-THU");

        List<FieldChange> changes = describeChanges(bus, previousBusId, previousBusNumber,
                previousBusNameBn, previousBusNameEn,
                previousDeparture, previousArrival, previousDirection,
                previousRouteNameBn, previousRouteNameEn, previousDays, schedule);

        if (changes.isEmpty()) {
            log.info("Schedule {} saved with no effective change — riders not notified", id);
            return;
        }

        log.info("Schedule {} changed: {}", id, changes);
        eventPublisher.publishEvent(ScheduleChangedEvent.from(Action.UPDATED, schedule, changes));
    }

    /**
     * Field-by-field diff of the snapshot taken before the mutation against the
     * schedule as it now stands. Empty means "nothing a rider cares about moved"
     * — the admin gets their 200, nobody gets a push.
     *
     * <p>Diff values are structured ({@link FieldChange}), not sentences: the
     * push dispatcher localizes the field labels per device language at send
     * time. Values are kept as plain display data — times as "HH:mm", missing
     * values as null (rendered as a localized placeholder).
     */
    private List<FieldChange> describeChanges(Bus bus,
                                              Long previousBusId, String previousBusNumber,
                                              String previousBusNameBn, String previousBusNameEn,
                                              LocalTime previousDeparture, LocalTime previousArrival,
                                              String previousDirection, String previousRouteNameBn,
                                              String previousRouteNameEn, String previousDays,
                                              Schedule schedule) {
        List<FieldChange> changes = new ArrayList<>();

        if (!Objects.equals(previousBusId, bus.getId())) {
            changes.add(new FieldChange(Field.BUS_NUMBER, previousBusNumber, bus.getBusNumber()));
        }
        if (!Objects.equals(previousBusNameBn, bus.getBusNameBn())) {
            changes.add(new FieldChange(Field.BUS_NAME_BN, text(previousBusNameBn), text(bus.getBusNameBn())));
        }
        if (!Objects.equals(previousBusNameEn, bus.getBusNameEn())) {
            changes.add(new FieldChange(Field.BUS_NAME_EN, text(previousBusNameEn), text(bus.getBusNameEn())));
        }
        if (!Objects.equals(previousDeparture, schedule.getDepartureTime())) {
            changes.add(new FieldChange(Field.DEPARTURE, time(previousDeparture), time(schedule.getDepartureTime())));
        }
        if (!Objects.equals(previousArrival, schedule.getArrivalTime())) {
            changes.add(new FieldChange(Field.ARRIVAL, time(previousArrival), time(schedule.getArrivalTime())));
        }
        if (!Objects.equals(previousDirection, schedule.getDirection())) {
            changes.add(new FieldChange(Field.DIRECTION, text(previousDirection), text(schedule.getDirection())));
        }
        Route route = schedule.getRoute();
        if (!Objects.equals(previousRouteNameBn, route != null ? route.getNameBn() : null)) {
            changes.add(new FieldChange(Field.ROUTE_NAME_BN, text(previousRouteNameBn),
                    text(route != null ? route.getNameBn() : null)));
        }
        if (!Objects.equals(previousRouteNameEn, route != null ? route.getNameEn() : null)) {
            changes.add(new FieldChange(Field.ROUTE_NAME_EN, text(previousRouteNameEn),
                    text(route != null ? route.getNameEn() : null)));
        }
        if (!Objects.equals(previousDays, schedule.getDays())) {
            changes.add(new FieldChange(Field.DAYS, text(previousDays), text(schedule.getDays())));
        }

        return changes;
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String time(LocalTime value) {
        return value == null ? null : String.format("%02d:%02d", value.getHour(), value.getMinute());
    }

    private Route findRoute(Long routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));
    }

    @Transactional
    public void deleteSchedule(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
        // Snapshot while the row and its lazy bus are still loaded.
        ScheduleChangedEvent event = ScheduleChangedEvent.from(Action.DELETED, schedule, List.of());
        scheduleRepository.delete(schedule);
        eventPublisher.publishEvent(event);
    }

    private ScheduleResponse mapToScheduleResponse(Schedule schedule) {
        Route route = schedule.getRoute();
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .busId(schedule.getBus().getId())
                .busNumber(schedule.getBus().getBusNumber())
                .busNameBn(schedule.getBus().getBusNameBn())
                .busNameEn(schedule.getBus().getBusNameEn())
                .category(schedule.getBus().getCategory())
                .departureTime(schedule.getDepartureTime().toString())
                .arrivalTime(schedule.getArrivalTime() != null ? schedule.getArrivalTime().toString() : null)
                .direction(schedule.getDirection())
                .routeId(route != null ? route.getId() : null)
                .routeNameBn(route != null ? route.getNameBn() : null)
                .routeNameEn(route != null ? route.getNameEn() : null)
                .days(schedule.getDays())
                .isActive(schedule.getIsActive())
                .build();
    }
}
