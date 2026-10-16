package com.cou.bustracker.controller.admin;

import com.cou.bustracker.dto.request.CreateAdminRequest;
import com.cou.bustracker.dto.request.UpdateAdminProfileRequest;
import com.cou.bustracker.dto.response.AdminProfileResponse;
import com.cou.bustracker.service.AdminManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/manage")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "Manage other admin accounts")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    @GetMapping
    @Operation(summary = "Get all admin accounts")
    public ResponseEntity<List<AdminProfileResponse>> getAllAdmins() {
        return ResponseEntity.ok(adminManagementService.getAllAdmins());
    }

    @PostMapping
    @Operation(summary = "Create an admin account")
    public ResponseEntity<AdminProfileResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminManagementService.createAdmin(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update another admin account")
    public ResponseEntity<AdminProfileResponse> updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminProfileRequest request) {
        return ResponseEntity.ok(adminManagementService.updateAdmin(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete another admin account")
    public ResponseEntity<Void> deleteAdmin(Authentication authentication, @PathVariable Long id) {
        adminManagementService.deleteAdmin(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
