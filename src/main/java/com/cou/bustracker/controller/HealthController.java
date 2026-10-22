package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Public, unauthenticated status endpoint (GET /api/health).
 *
 * API liveness is implicit: reaching this handler means the API answered.
 * Database liveness is delegated to Spring Boot Actuator's built-in
 * DataSource health indicator (validation query through the connection
 * pool) instead of hand-rolling a JDBC check — the core {@link HealthEndpoint}
 * bean is read directly, so component statuses are always available
 * regardless of the web layer's show-details setting.
 *
 * Responds 503 when the DB check fails so load balancers and docker
 * healthchecks can act on the status code alone.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Public API/database status and admin panel links (no auth required)")
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    @Value("${app.links.admin-panel}")
    private String adminPanelUrl;

    @Value("${app.links.super-admin-panel}")
    private String superAdminPanelUrl;

    @GetMapping
    @Operation(summary = "Service status: API + database health, admin panel URLs")
    public ResponseEntity<HealthResponse> health() {
        // Actuator registers the DataSource indicator under the "db" key.
        HealthComponent db = healthEndpoint.healthForPath("db");
        String databaseStatus = db != null ? db.getStatus().getCode() : "UNKNOWN";

        // The API answered (this handler), so api is UP by definition;
        // overall status mirrors the DB check only.
        boolean up = "UP".equals(databaseStatus);
        HealthResponse body = HealthResponse.builder()
                .status(up ? "UP" : "DOWN")
                .timestamp(Instant.now().toString())
                .api("UP")
                .database(databaseStatus)
                .adminPanel(adminPanelUrl)
                .superAdminPanel(superAdminPanelUrl)
                .build();

        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }
}
