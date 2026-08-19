package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.CreateBusRequest;
import com.cou.bustracker.dto.request.UpdateBusRequest;
import com.cou.bustracker.dto.response.BusDetailResponse;
import com.cou.bustracker.dto.response.BusResponse;
import com.cou.bustracker.dto.response.ScheduleResponse;
import com.cou.bustracker.entity.Bus;
import com.cou.bustracker.entity.Schedule;
import com.cou.bustracker.entity.TrackerLink;
import com.cou.bustracker.repository.BusRepository;
import com.cou.bustracker.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusService {

    private final BusRepository busRepository;

    public List<BusResponse> getAllActiveBuses() {
        List<Bus> buses = busRepository.findByIsActiveTrue();
        return buses.stream()
                .map(this::mapToBusResponse)
                .collect(Collectors.toList());
    }

    public List<BusResponse> getBusesByCategory(String category) {
        List<Bus> buses = busRepository.findByCategoryAndIsActiveTrue(category);
        return buses.stream()
                .map(this::mapToBusResponse)
                .collect(Collectors.toList());
    }

    public List<BusResponse> getAllBusesForAdmin() {
        List<Bus> buses = busRepository.findAllByOrderByCreatedAtDesc();
        return buses.stream()
                .map(this::mapToBusResponse)
                .collect(Collectors.toList());
    }

    public void toggleBus(Long id) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));
        bus.setIsActive(!bus.getIsActive());
        busRepository.save(bus);
    }

    public BusDetailResponse getBusById(Long id) {
        Bus bus = busRepository.findByIdWithTrackerLinkAndSchedules(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));
        return mapToBusDetailResponse(bus);
    }

    @Transactional
    public BusResponse createBus(CreateBusRequest request) {
        Bus bus = Bus.builder()
                .busNumber(request.getBusNumber())
                .busName(request.getBusName())
                .category(request.getCategory())
                .route(request.getRoute())
                .driverName(request.getDriverName())
                .driverPhone(request.getDriverPhone())
                .busImageUrl(request.getBusImageUrl())
                .isActive(true)
                .build();

        Bus savedBus = busRepository.save(bus);
        return mapToBusResponse(savedBus);
    }

    @Transactional
    public void updateBus(Long id, UpdateBusRequest request) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));

        if (request.getBusNumber() != null) bus.setBusNumber(request.getBusNumber());
        if (request.getBusName() != null) bus.setBusName(request.getBusName());
        if (request.getCategory() != null) bus.setCategory(request.getCategory());
        if (request.getRoute() != null) bus.setRoute(request.getRoute());
        if (request.getDriverName() != null) bus.setDriverName(request.getDriverName());
        if (request.getDriverPhone() != null) bus.setDriverPhone(request.getDriverPhone());
        if (request.getBusImageUrl() != null) bus.setBusImageUrl(request.getBusImageUrl());
        if (request.getIsActive() != null) bus.setIsActive(request.getIsActive());

        busRepository.save(bus);
    }

    @Transactional
    public void deleteBus(Long id) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));
        busRepository.delete(bus);
    }

    private BusResponse mapToBusResponse(Bus bus) {
        String trackerUrl = null;
        if (bus.getTrackerLink() != null) {
            trackerUrl = bus.getTrackerLink().getTrackerUrl();
        }

        return BusResponse.builder()
                .id(bus.getId())
                .busNumber(bus.getBusNumber())
                .busName(bus.getBusName())
                .category(bus.getCategory())
                .route(bus.getRoute())
                .driverName(bus.getDriverName())
                .driverPhone(bus.getDriverPhone())
                .busImageUrl(bus.getBusImageUrl())
                .trackerUrl(trackerUrl)
                .isActive(bus.getIsActive())
                .build();
    }

    private BusDetailResponse mapToBusDetailResponse(Bus bus) {
        String trackerUrl = null;
        if (bus.getTrackerLink() != null) {
            trackerUrl = bus.getTrackerLink().getTrackerUrl();
        }

        List<ScheduleResponse> schedules = bus.getSchedules() != null ?
                bus.getSchedules().stream()
                        .map(this::mapToScheduleResponse)
                        .collect(Collectors.toList()) :
                List.of();

        return BusDetailResponse.builder()
                .id(bus.getId())
                .busNumber(bus.getBusNumber())
                .busName(bus.getBusName())
                .category(bus.getCategory())
                .route(bus.getRoute())
                .driverName(bus.getDriverName())
                .driverPhone(bus.getDriverPhone())
                .busImageUrl(bus.getBusImageUrl())
                .trackerUrl(trackerUrl)
                .isActive(bus.getIsActive())
                .schedules(schedules)
                .build();
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
                .build();
    }
}
