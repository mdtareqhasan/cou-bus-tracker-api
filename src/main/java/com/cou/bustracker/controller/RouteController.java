package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.RouteResponse;
import com.cou.bustracker.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public route reference data.
 *
 * <p>Every route carries both language forms (Bangla + English) and the
 * client renders whichever matches its locale. Unauthenticated for the same
 * reason as the buses endpoint: the data is non-sensitive and the Flutter app
 * needs it on the home screen before any login step.
 */
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Tag(name = "Route API", description = "Public route endpoints")
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    @Operation(summary = "Get all active routes", description = "Retrieve all active routes with Bangla and English names")
    public ResponseEntity<List<RouteResponse>> getRoutes() {
        return ResponseEntity.ok(routeService.getActiveRoutes());
    }
}
