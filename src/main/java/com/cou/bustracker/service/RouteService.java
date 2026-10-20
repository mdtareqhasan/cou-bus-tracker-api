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
        return routeRepository.findByIsActiveTrue().stream()
                .map(RouteService::toResponse)
                .collect(Collectors.toList());
    }

    /** Every route, active and retired — the admin listing. */
    public List<RouteResponse> getAllRoutes() {
        return routeRepository.findAllByOrderByIdAsc().stream()
                .map(RouteService::toResponse)
                .collect(Collectors.toList());
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
        return toResponse(routeRepository.save(route));
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
        return toResponse(routeRepository.save(route));
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

    private Route findRoute(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ROUTE_NOT_FOUND));
    }

    private static RouteResponse toResponse(Route route) {
        return RouteResponse.builder()
                .id(route.getId())
                .nameBn(route.getNameBn())
                .nameEn(route.getNameEn())
                .isActive(route.getIsActive())
                .build();
    }
}
