package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.CreateScheduleRequest;
import com.cou.bustracker.dto.response.ScheduleResponse;
import com.cou.bustracker.entity.Bus;
import com.cou.bustracker.entity.Schedule;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.BusRepository;
import com.cou.bustracker.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final BusRepository busRepository;

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

        scheduleRepository.save(schedule);
    }

    @Transactional
    public void updateSchedule(Long id, CreateScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + request.getBusId()));
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
    }

    @Transactional
    public void deleteSchedule(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
        scheduleRepository.delete(schedule);
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
