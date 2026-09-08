package com.cou.bustracker.controller.superadmin;

import com.cou.bustracker.dto.superadmin.SuperAdminLoginRequest;
import com.cou.bustracker.dto.superadmin.SuperAdminLoginResponse;
import com.cou.bustracker.service.SuperAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/super-admin/auth")
@RequiredArgsConstructor
@Tag(name = "Super Admin Auth", description = "Super admin authentication endpoints")
public class SuperAdminAuthController {

    private final SuperAdminService superAdminService;

    @PostMapping("/login")
    @Operation(summary = "Super admin login")
    public ResponseEntity<SuperAdminLoginResponse> login(@Valid @RequestBody SuperAdminLoginRequest request) {
        return ResponseEntity.ok(superAdminService.login(request));
    }
}