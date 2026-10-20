package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.CreateScheduleRequest;
import com.cou.bustracker.entity.Bus;
import com.cou.bustracker.entity.Route;
import com.cou.bustracker.entity.Schedule;
import com.cou.bustracker.event.ScheduleChangedEvent;
import com.cou.bustracker.repository.BusRepository;
import com.cou.bustracker.repository.RouteRepository;
import com.cou.bustracker.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the "arrival time is not rider-facing" rule at the source: the diff
 * never reports an arrival change, so an arrival-only edit publishes no
 * {@link ScheduleChangedEvent} at all and riders are never pushed to.
 */
class ScheduleServiceTest {

    private ScheduleRepository scheduleRepository;
    private BusRepository busRepository;
    private RouteRepository routeRepository;
    private ApplicationEventPublisher eventPublisher;
    private ScheduleService scheduleService;

    private Bus bus;
    private Route route;
    private Schedule schedule;

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(ScheduleRepository.class);
        busRepository = mock(BusRepository.class);
        routeRepository = mock(RouteRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        scheduleService = new ScheduleService(scheduleRepository, busRepository, routeRepository, eventPublisher);

        bus = Bus.builder().id(1L).busNumber("BUS 03").busNameBn("সিওই ব্লু").busNameEn("COU Blue")
                .category("BLUE").build();
        route = Route.builder().id(4L).nameEn("Kandirpar → Campus").nameBn("কান্দিরপাড় → ক্যাম্পাস").build();
        schedule = Schedule.builder().id(9L).bus(bus).route(route)
                .departureTime(LocalTime.of(7, 0)).arrivalTime(LocalTime.of(8, 0))
                .direction("UP").days("SUN-THU").isActive(true).build();

        when(scheduleRepository.findById(9L)).thenReturn(Optional.of(schedule));
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
        when(routeRepository.findById(4L)).thenReturn(Optional.of(route));
    }

    @Test
    void arrivalOnlyEditPublishesNoEvent() {
        scheduleService.updateSchedule(9L, request("07:00", "08:30"));

        // The arrival value is still persisted — it is just not rider-facing.
        assertEquals(LocalTime.of(8, 30), schedule.getArrivalTime());
        verify(eventPublisher, never()).publishEvent(any(ScheduleChangedEvent.class));
    }

    /** A schedule edit that touches nothing but the times. */
    private static CreateScheduleRequest request(String departure, String arrival) {
        return new CreateScheduleRequest(1L, null, null, departure, arrival, "UP", 4L, "SUN-THU");
    }
}
