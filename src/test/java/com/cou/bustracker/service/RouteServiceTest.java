package com.cou.bustracker.service;

import com.cou.bustracker.dto.response.RouteResponse;
import com.cou.bustracker.entity.Route;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.repository.RouteRepository;
import com.cou.bustracker.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins down the pairing invariants behind opposite-route linking: reciprocity
 * (both sides point at each other), replacement (an old partner is unpaired,
 * never left dangling), and healing of the stale one-sided links a crash
 * between the two writes could leave behind — without which a relink would
 * trip the DB's UNIQUE(opposite_route_id) index instead of fixing itself.
 */
class RouteServiceTest {

    private RouteRepository routeRepository;
    private ScheduleRepository scheduleRepository;
    private RouteService service;

    @BeforeEach
    void setUp() {
        routeRepository = mock(RouteRepository.class);
        scheduleRepository = mock(ScheduleRepository.class);
        service = new RouteService(routeRepository, scheduleRepository);
        when(routeRepository.save(any(Route.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void linkOppositePointsBothRoutesAtEachOther() {
        Route forward = route(1L, "Kandirpar → Campus");
        Route backward = route(8L, "Campus → Kandirpar");
        stubRoute(1L, forward);
        stubRoute(8L, backward);

        RouteResponse response = service.linkOpposite(1L, 8L);

        assertEquals(Long.valueOf(8L), forward.getOppositeRouteId());
        assertEquals(Long.valueOf(1L), backward.getOppositeRouteId());
        // The response carries the link so the admin panel can redraw at once.
        assertEquals(Long.valueOf(8L), response.getOppositeRouteId());
    }

    @Test
    void linkOppositeRejectsSelfLink() {
        ApiException ex = assertThrows(ApiException.class, () -> service.linkOpposite(1L, 1L));

        assertEquals(ErrorCode.ROUTE_OPPOSITE_SELF, ex.getErrorCode());
    }

    @Test
    void linkOppositeUnpairsTheOldPartnerInsteadOfLeavingItDangling() {
        Route forward = route(1L, "Kandirpar → Campus");
        forward.setOppositeRouteId(9L);
        Route oldPartner = route(9L, "Kandirpar → Tomsombridge → Campus");
        oldPartner.setOppositeRouteId(1L);
        Route backward = route(8L, "Campus → Kandirpar");
        stubRoute(1L, forward);
        stubRoute(9L, oldPartner);
        stubRoute(8L, backward);

        service.linkOpposite(1L, 8L);

        assertEquals(Long.valueOf(8L), forward.getOppositeRouteId());
        assertEquals(Long.valueOf(1L), backward.getOppositeRouteId());
        assertNull(oldPartner.getOppositeRouteId(),
                "the replaced partner must be unpaired, not left pointing at a route that moved on");
    }

    @Test
    void linkOppositeHealsStaleOneSidedLinkOnTheTarget() {
        // A crash between the two writes can leave a third route pointing at
        // our target with no back-pointer. Relinking must clear it rather than
        // raise a constraint violation — the UNIQUE index sees what reciprocity
        // broke.
        Route forward = route(1L, "Kandirpar → Campus");
        Route backward = route(8L, "Campus → Kandirpar");
        Route stale = route(9L, "Kandirpar → Tomsombridge → Campus");
        stale.setOppositeRouteId(8L);
        stubRoute(1L, forward);
        stubRoute(8L, backward);
        when(routeRepository.findByOppositeRouteId(1L)).thenReturn(Optional.empty());
        when(routeRepository.findByOppositeRouteId(8L)).thenReturn(Optional.of(stale));

        service.linkOpposite(1L, 8L);

        assertNull(stale.getOppositeRouteId(),
                "the stale inbound link must be cleared before the new pairing is written");
        assertEquals(Long.valueOf(1L), backward.getOppositeRouteId());
    }

    @Test
    void unlinkOppositeClearsBothSides() {
        Route forward = route(1L, "Kandirpar → Campus");
        forward.setOppositeRouteId(8L);
        Route backward = route(8L, "Campus → Kandirpar");
        backward.setOppositeRouteId(1L);
        stubRoute(1L, forward);
        stubRoute(8L, backward);

        RouteResponse response = service.unlinkOpposite(1L);

        assertNull(forward.getOppositeRouteId());
        assertNull(backward.getOppositeRouteId());
        assertNull(response.getOppositeRouteId());
    }

    @Test
    void unlinkOfNeverLinkedRouteIsNotAnError() {
        Route lonely = route(3L, "Campus → Policeline → Eidgah");
        stubRoute(3L, lonely);

        RouteResponse response = service.unlinkOpposite(3L);

        assertNull(response.getOppositeRouteId());
    }

    @Test
    void listingStampsTheActiveScheduleCountOntoEachRoute() {
        Route route = route(1L, "Kandirpar → Campus");
        when(routeRepository.findByIsActiveTrue()).thenReturn(List.of(route));
        when(scheduleRepository.countActiveSchedulesByRoute()).thenReturn(Map.of(1L, 3L));

        assertEquals(Long.valueOf(3L),
                service.getActiveRoutes().get(0).getActiveScheduleCount(),
                "the Route tab badge must match what the schedules endpoint serves");
    }

    private void stubRoute(Long id, Route route) {
        when(routeRepository.findById(id)).thenReturn(Optional.of(route));
    }

    private static Route route(Long id, String nameEn) {
        return Route.builder()
                .id(id)
                .nameBn("টেস্ট")
                .nameEn(nameEn)
                .isActive(true)
                .build();
    }
}
