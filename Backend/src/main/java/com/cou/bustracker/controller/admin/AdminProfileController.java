package com.cou.bustracker.controller.admin;

import com.cou.bustracker.dto.request.UpdateAdminProfileRequest;
import com.cou.bustracker.dto.response.AdminProfileResponse;
import com.cou.bustracker.service.AdminProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
@Tag(name = "Admin Profile", description = "Authenticated admin profile endpoints")
public class AdminProfileController {

    private final AdminProfileService adminProfileService;

    @GetMapping
    @Operation(summary = "Get the signed-in admin profile")
    public ResponseEntity<AdminProfileResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(adminProfileService.getProfile(authentication.getName()));
    }

    @PutMapping
    @Operation(summary = "Update the signed-in admin profile")
    public ResponseEntity<AdminProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateAdminProfileRequest request
    ) {
        return ResponseEntity.ok(adminProfileService.updateProfile(authentication.getName(), request));
    }
}
