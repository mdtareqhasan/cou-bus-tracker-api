package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated status endpoint (GET /api/health).
 *
 * api: "ok" by definition (this handler answered). db: Spring Boot
 * Actuator's built-in DataSource indicator, read via the core
 * {@link HealthEndpoint} bean. Responds 503 when the DB check fails.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Public API/database status (no auth required)")
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    @GetMapping
    @Operation(summary = "Service status: api + db")
    public ResponseEntity<HealthResponse> health() {
        HealthComponent db = healthEndpoint.healthForPath("db");
        boolean up = db != null && "UP".equals(db.getStatus().getCode());

        HealthResponse body = HealthResponse.builder()
                .api("ok")
                .db(up ? "ok" : "down")
                .build();

        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }
}
