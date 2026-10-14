package com.cou.bustracker.controller.superadmin;

import com.cou.bustracker.dto.superadmin.SuperAdminCreateRequest;
import com.cou.bustracker.dto.superadmin.SuperAdminResponse;
import com.cou.bustracker.dto.superadmin.SuperAdminUpdateRequest;
import com.cou.bustracker.service.SuperAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/manage")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Super Admin Management", description = "Manage super admin accounts")
public class SuperAdminManagementController {

    private final SuperAdminService superAdminService;

    @GetMapping
    @Operation(summary = "List all super admins")
    public ResponseEntity<List<SuperAdminResponse>> getAll() {
        return ResponseEntity.ok(superAdminService.getAllSuperAdmins());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific super admin")
    public ResponseEntity<SuperAdminResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(superAdminService.getSuperAdminById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new super admin")
    public ResponseEntity<SuperAdminResponse> create(@Valid @RequestBody SuperAdminCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(superAdminService.createSuperAdmin(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a super admin")
    public ResponseEntity<SuperAdminResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SuperAdminUpdateRequest request) {
        return ResponseEntity.ok(superAdminService.updateSuperAdmin(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a super admin")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        superAdminService.deleteSuperAdmin(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}