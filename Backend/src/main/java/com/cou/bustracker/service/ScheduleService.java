package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.CreateScheduleRequest;
import com.cou.bustracker.dto.response.ScheduleResponse;
import com.cou.bustracker.entity.Bus;
import com.cou.bustracker.entity.Schedule;
import com.cou.bustracker.event.ScheduleChangedEvent;
import com.cou.bustracker.event.ScheduleChangedEvent.Action;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.BusRepository;
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
        if (request.getBusName() != null && !request.getBusName().isBlank()) {
            bus.setBusName(request.getBusName());
        }

        Schedule schedule = Schedule.builder()
                .bus(bus)
                .departureTime(LocalTime.parse(request.getDepartureTime()))
                .arrivalTime(request.getArrivalTime() != null ? LocalTime.parse(request.getArrivalTime()) : null)
                .direction(request.getDirection())
                .startPoint(request.getStartPoint())
                .endPoint(request.getEndPoint())
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

        // Snapshot everything first: busName is mutated below, and when the
        // request points at the same bus, `bus` IS schedule.getBus() (same
        // persistence-context instance), so reading afterwards would hide it.
        final Long previousBusId = schedule.getBus().getId();
        final String previousBusNumber = schedule.getBus().getBusNumber();
        final String previousBusName = bus.getBusName();
        final LocalTime previousDeparture = schedule.getDepartureTime();
        final LocalTime previousArrival = schedule.getArrivalTime();
        final String previousDirection = schedule.getDirection();
        final String previousStartPoint = schedule.getStartPoint();
        final String previousEndPoint = schedule.getEndPoint();
        final String previousDays = schedule.getDays();

        if (request.getBusName() != null && !request.getBusName().isBlank()) {
            bus.setBusName(request.getBusName());
        }

        schedule.setBus(bus);
        schedule.setDepartureTime(LocalTime.parse(request.getDepartureTime()));
        schedule.setArrivalTime(request.getArrivalTime() != null && !request.getArrivalTime().isBlank()
                ? LocalTime.parse(request.getArrivalTime()) : null);
        schedule.setDirection(request.getDirection());
        schedule.setStartPoint(request.getStartPoint());
        schedule.setEndPoint(request.getEndPoint());
        schedule.setDays(request.getDays() != null ? request.getDays() : "SUN-THU");

        List<String> changes = describeChanges(bus, previousBusId, previousBusNumber, previousBusName,
                previousDeparture, previousArrival, previousDirection,
                previousStartPoint, previousEndPoint, previousDays, schedule);

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
     */
    private List<String> describeChanges(Bus bus,
                                         Long previousBusId, String previousBusNumber, String previousBusName,
                                         LocalTime previousDeparture, LocalTime previousArrival,
                                         String previousDirection, String previousStartPoint,
                                         String previousEndPoint, String previousDays,
                                         Schedule schedule) {
        List<String> changes = new ArrayList<>();

        if (!Objects.equals(previousBusId, bus.getId())) {
            changes.add("bus: " + previousBusNumber + " → " + bus.getBusNumber());
        }
        if (!Objects.equals(previousBusName, bus.getBusName())) {
            changes.add("bus name: " + text(previousBusName) + " → " + text(bus.getBusName()));
        }
        if (!Objects.equals(previousDeparture, schedule.getDepartureTime())) {
            changes.add("departure: " + time(previousDeparture) + " → " + time(schedule.getDepartureTime()));
        }
        if (!Objects.equals(previousArrival, schedule.getArrivalTime())) {
            changes.add("arrival: " + time(previousArrival) + " → " + time(schedule.getArrivalTime()));
        }
        if (!Objects.equals(previousDirection, schedule.getDirection())) {
            changes.add("direction: " + text(previousDirection) + " → " + text(schedule.getDirection()));
        }
        if (!Objects.equals(previousStartPoint, schedule.getStartPoint())) {
            changes.add("start: " + text(previousStartPoint) + " → " + text(schedule.getStartPoint()));
        }
        if (!Objects.equals(previousEndPoint, schedule.getEndPoint())) {
            changes.add("end: " + text(previousEndPoint) + " → " + text(schedule.getEndPoint()));
        }
        if (!Objects.equals(previousDays, schedule.getDays())) {
            changes.add("days: " + text(previousDays) + " → " + text(schedule.getDays()));
        }

        return changes;
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private static String time(LocalTime value) {
        return value == null ? "none" : String.format("%02d:%02d", value.getHour(), value.getMinute());
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
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .busId(schedule.getBus().getId())
                .busNumber(schedule.getBus().getBusNumber())
                .busName(schedule.getBus().getBusName())
                .category(schedule.getBus().getCategory())
                .departureTime(schedule.getDepartureTime().toString())
                .arrivalTime(schedule.getArrivalTime() != null ? schedule.getArrivalTime().toString() : null)
                .direction(schedule.getDirection())
                .startPoint(schedule.getStartPoint())
                .endPoint(schedule.getEndPoint())
                .days(schedule.getDays())
                .isActive(schedule.getIsActive())
                .build();
    }
}
