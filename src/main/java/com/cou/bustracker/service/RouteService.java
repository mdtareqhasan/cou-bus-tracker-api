package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.RouteRequest;
import com.cou.bustracker.dto.response.RouteResponse;
import com.cou.bustracker.entity.Route;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.repository.RouteRepository;
import com.cou.bustracker.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Route reference data: the public listing the Flutter app renders and the
 * admin CRUD behind the panel's Routes tab.
 *
 * <p>Two rules shape the write paths (mirroring the academic reference data):
 *
 * <ul>
 *   <li><b>Deactivate, don't delete.</b> Schedules point at routes by FK, so
 *       deleting a route that is still scheduled fails on the constraint
 *       (ON DELETE RESTRICT). Deleting an in-use route is rejected with a
 *       conflict; {@code is_active = false} hides it from new assignments
 *       instead.</li>
 *   <li><b>Both language names are unique.</b> Mirrors the DB constraints, but
 *       is checked in the service so the caller gets a stable error code
 *       rather than a raw constraint violation.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RouteService {

    private final RouteRepository routeRepository;
    private final ScheduleRepository scheduleRepository;

    /** Active routes only — what the public buses endpoints and app consume. */
    public List<RouteResponse> getActiveRoutes() {
        return withScheduleCounts(routeRepository.findByIsActiveTrue());
    }

    /** Every route, active and retired — the admin listing. */
    public List<RouteResponse> getAllRoutes() {
        return withScheduleCounts(routeRepository.findAllByOrderByIdAsc());
    }

    public RouteResponse createRoute(RouteRequest request) {
        String nameBn = request.getNameBn().trim();
        String nameEn = request.getNameEn().trim();
        if (routeRepository.existsByNameBn(nameBn) || routeRepository.existsByNameEn(nameEn)) {
            throw new ApiException(ErrorCode.ROUTE_NAME_EXISTS);
        }
        Route route = Route.builder()
                .nameBn(nameBn)
                .nameEn(nameEn)
                .isActive(true)
                .build();
        return withScheduleCounts(List.of(routeRepository.save(route))).get(0);
    }

    public RouteResponse updateRoute(Long id, RouteRequest request) {
        Route route = findRoute(id);
        String nameBn = request.getNameBn().trim();
        String nameEn = request.getNameEn().trim();
        if (routeRepository.existsByNameBnAndIdNot(nameBn, id)
                || routeRepository.existsByNameEnAndIdNot(nameEn, id)) {
            throw new ApiException(ErrorCode.ROUTE_NAME_EXISTS);
        }
        route.setNameBn(nameBn);
        route.setNameEn(nameEn);
        return withScheduleCounts(List.of(routeRepository.save(route))).get(0);
    }

    public void deleteRoute(Long id) {
        Route route = findRoute(id);
        long schedules = scheduleRepository.countByRouteId(id);
        if (schedules > 0) {
            throw new ApiException(ErrorCode.ROUTE_IN_USE,
                    "This route is used by " + schedules + " schedule(s). "
                            + "Deactivate it instead of deleting");
        }
        routeRepository.delete(route);
    }

    /** Flips {@code is_active}; returns the new state for the success message. */
    public boolean toggleRoute(Long id) {
        Route route = findRoute(id);
        route.setIsActive(!Boolean.TRUE.equals(route.getIsActive()));
        routeRepository.save(route);
        return Boolean.TRUE.equals(route.getIsActive());
    }

    /**
     * Pairs two routes as the two physical directions of one corridor.
     *
     * <p>Reciprocity is the whole point: after this call both rows point at
     * each other, so the client can render the pair from either side. If
     * either route was already paired with someone else the old partner is
     * unpaired first (one route has at most one opposite — the DB enforces it
     * with a UNIQUE index) — the admin UI asks for confirmation before
     * replacing an existing link.
     */
    public RouteResponse linkOpposite(Long routeId, Long oppositeRouteId) {
        if (routeId.equals(oppositeRouteId)) {
            throw new ApiException(ErrorCode.ROUTE_OPPOSITE_SELF);
        }
        Route route = findRoute(routeId);
        Route opposite = findRoute(oppositeRouteId);

        // Free both sides first: an existing partner (or a stale one-sided
        // link, where a third route still points at one of these) would trip
        // the DB's UNIQUE(opposite_route_id). One route has at most one
        // opposite — the admin UI asks for confirmation before replacing.
        unlinkPartner(route);
        unlinkPartner(opposite);
        clearStaleInboundLinks(routeId);
        clearStaleInboundLinks(oppositeRouteId);

        route.setOppositeRouteId(oppositeRouteId);
        opposite.setOppositeRouteId(routeId);
        routeRepository.save(route);
        routeRepository.save(opposite);
        return withScheduleCounts(List.of(route)).get(0);
    }

    /** Breaks a pairing, clearing both sides. Unpaired routes are the norm, so this is not an error. */
    public RouteResponse unlinkOpposite(Long routeId) {
        Route route = findRoute(routeId);
        unlinkPartner(route);
        clearStaleInboundLinks(routeId);
        route.setOppositeRouteId(null);
        routeRepository.save(route);
        return withScheduleCounts(List.of(route)).get(0);
    }

    /**
     * Clears the partner's back-pointer to {@code route} when it has one. A
     * partner that points elsewhere (or nowhere) is left alone — its link is
     * its own business.
     */
    private void unlinkPartner(Route route) {
        Long partnerId = route.getOppositeRouteId();
        if (partnerId == null) {
            return;
        }
        routeRepository.findById(partnerId).ifPresent(partner -> {
            if (route.getId().equals(partner.getOppositeRouteId())) {
                partner.setOppositeRouteId(null);
                routeRepository.save(partner);
            }
        });
    }

    /**
     * Clears any route whose {@code opposite_route_id} points at
     * {@code routeId} while {@code routeId} doesn't point back — a stale
     * one-sided link left by a crash between the two writes. Without this,
     * relinking would raise a constraint violation instead of healing.
     */
    private void clearStaleInboundLinks(Long routeId) {
        routeRepository.findByOppositeRouteId(routeId).ifPresent(inbound -> {
            inbound.setOppositeRouteId(null);
            routeRepository.save(inbound);
        });
    }

    /** Stamps the active-schedule count onto each response in one extra query. */
    private List<RouteResponse> withScheduleCounts(List<Route> routes) {
        Map<Long, Long> counts = scheduleRepository.countActiveSchedulesByRoute();
        return routes.stream()
                .map(route -> toResponse(route, counts.getOrDefault(route.getId(), 0L)))
                .collect(Collectors.toList());
    }

    private Route findRoute(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ROUTE_NOT_FOUND));
    }

    private static RouteResponse toResponse(Route route, Long activeScheduleCount) {
        return RouteResponse.builder()
                .id(route.getId())
                .nameBn(route.getNameBn())
                .nameEn(route.getNameEn())
                .isActive(route.getIsActive())
                .oppositeRouteId(route.getOppositeRouteId())
                .activeScheduleCount(activeScheduleCount)
                .build();
    }
}
