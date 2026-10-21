package com.cou.bustracker.controller.admin;

import com.cou.bustracker.dto.request.RouteRequest;
import com.cou.bustracker.dto.response.MessageResponse;
import com.cou.bustracker.dto.response.RouteResponse;
import com.cou.bustracker.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin CRUD for the bus routes table.
 *
 * <p>Listings include retired rows; the public {@code /api/routes} endpoint
 * keeps serving only active ones. Deleting a route that existing schedules
 * reference is rejected (FK RESTRICT) — deactivate it instead.
 */
@RestController
@RequestMapping("/api/admin/routes")
@RequiredArgsConstructor
@Tag(name = "Admin Route Management", description = "Admin route management (JWT required)")
public class AdminRouteController {

    private final RouteService routeService;

    @GetMapping
    @Operation(summary = "Get all routes (Admin)", description = "All routes, including retired ones")
    public ResponseEntity<List<RouteResponse>> getAllRoutes() {
        return ResponseEntity.ok(routeService.getAllRoutes());
    }

    @PostMapping
    @Operation(summary = "Add route", description = "Create a route with Bangla and English names")
    public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeService.createRoute(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update route", description = "Rename a route's Bangla or English name")
    public ResponseEntity<RouteResponse> updateRoute(@PathVariable Long id,
                                                     @Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(routeService.updateRoute(id, request));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle route active status", description = "Retire or re-activate a route")
    public ResponseEntity<MessageResponse> toggleRoute(@PathVariable Long id) {
        boolean active = routeService.toggleRoute(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Route " + (active ? "activated" : "deactivated") + " successfully")
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete route", description = "Only allowed when no schedule references it")
    public ResponseEntity<MessageResponse> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Route deleted successfully")
                .build());
    }

    @PutMapping("/{id}/opposite/{oppositeId}")
    @Operation(summary = "Link two routes as opposite directions",
            description = "Pairs the routes both ways; replacing an existing pairing is allowed (the old partner is unlinked)")
    public ResponseEntity<RouteResponse> linkOpposite(@PathVariable Long id,
                                                      @PathVariable Long oppositeId) {
        return ResponseEntity.ok(routeService.linkOpposite(id, oppositeId));
    }

    @DeleteMapping("/{id}/opposite")
    @Operation(summary = "Unlink a route's opposite", description = "Clears the pairing on both sides; idempotent")
    public ResponseEntity<RouteResponse> unlinkOpposite(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.unlinkOpposite(id));
    }
}
