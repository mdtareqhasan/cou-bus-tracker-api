package com.cou.bustracker.service;

import com.cou.bustracker.dto.response.BusDetailResponse;
import com.cou.bustracker.entity.Bus;
import com.cou.bustracker.entity.Route;
import com.cou.bustracker.entity.Schedule;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.BusRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the payload the bus page relies on to grey out paused trips: the bus
 * detail endpoint must keep deactivated schedules in the list and mark them
 * with {@code isActive=false}. Dropping them would leave a rider who taps the
 * "service paused" push staring at a bus page that never mentions the trip.
 */
class BusServiceTest {

    private final BusRepository repository = mock(BusRepository.class);
    private final BusService service = new BusService(repository);

    @Test
    void busDetailKeepsPausedTripsAndFlagsThemInactive() {
        Bus bus = busWithTrips(trip(1L, true), trip(2L, false));
        when(repository.findByIdWithTrackerLinkAndSchedules(7L))
                .thenReturn(Optional.of(bus));

        BusDetailResponse detail = service.getBusById(7L);

        assertEquals(2, detail.getSchedules().size(), "paused trips must stay in the payload");
        assertTrue(detail.getSchedules().stream()
                        .anyMatch(s -> Boolean.TRUE.equals(s.getIsActive())),
                "running trip flagged active");
        assertTrue(detail.getSchedules().stream()
                        .anyMatch(s -> Boolean.FALSE.equals(s.getIsActive())),
                "paused trip flagged inactive");
    }

    @Test
    void scheduleCarriesItsRouteNames() {
        Bus bus = busWithTrips(trip(1L, true));
        when(repository.findByIdWithTrackerLinkAndSchedules(7L))
                .thenReturn(Optional.of(bus));

        BusDetailResponse detail = service.getBusById(7L);

        assertEquals(1, detail.getSchedules().size());
        assertFalse(detail.getSchedules().get(0).getRouteNameEn().isEmpty());
    }

    @Test
    void missingBusThrows() {
        when(repository.findByIdWithTrackerLinkAndSchedules(404L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getBusById(404L));
    }

    /** A bus whose schedules point back at it (the mapper dereferences them). */
    private static Bus busWithTrips(Schedule... trips) {
        Bus bus = Bus.builder()
                .id(7L)
                .busNumber("BUS 03")
                .category("BLUE")
                .isActive(true)
                .build();
        for (Schedule trip : trips) {
            trip.setBus(bus);
            bus.getSchedules().add(trip);
        }
        return bus;
    }

    private static Schedule trip(long id, boolean active) {
        return Schedule.builder()
                .id(id)
                .route(Route.builder()
                        .id(4L)
                        .nameBn("ক্যাম্পাস")
                        .nameEn("Campus")
                        .build())
                .departureTime(LocalTime.of(7, 0))
                .direction("UP")
                .days("SUN-THU")
                .isActive(active)
                .build();
    }
}
