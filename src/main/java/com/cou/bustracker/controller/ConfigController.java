package com.cou.bustracker.controller;

import com.cou.bustracker.dto.config.PublicConfigResponse;
import com.cou.bustracker.service.AppConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated endpoint consumed by the Flutter app on launch.
 * Returns runtime configuration such as API base URL, app version, and
 * maintenance state.
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Tag(name = "Public Config", description = "Unauthenticated runtime configuration for Flutter app")
public class ConfigController {

    private final AppConfigService appConfigService;

    @GetMapping
    @Operation(summary = "Public app configuration (no auth required)")
    public ResponseEntity<PublicConfigResponse> publicConfig() {
        return ResponseEntity.ok(appConfigService.getPublicConfig());
    }
}