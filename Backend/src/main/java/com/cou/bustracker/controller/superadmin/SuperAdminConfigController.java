package com.cou.bustracker.controller.superadmin;

import com.cou.bustracker.dto.config.AppConfigUpdateRequest;
import com.cou.bustracker.entity.AppConfig;
import com.cou.bustracker.service.AppConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Super Admin Config", description = "Manage runtime application configuration")
public class SuperAdminConfigController {

    private final AppConfigService appConfigService;

    @GetMapping
    @Operation(summary = "List all app config keys and values")
    public ResponseEntity<List<AppConfig>> list() {
        return ResponseEntity.ok(appConfigService.listAll());
    }

    @PutMapping
    @Operation(summary = "Update one or more app config values")
    public ResponseEntity<List<AppConfig>> update(
            @Valid @RequestBody AppConfigUpdateRequest request,
            Authentication authentication) {
        List<AppConfig> updated = appConfigService.updateConfig(request, authentication.getName());
        return ResponseEntity.ok(updated);
    }
}