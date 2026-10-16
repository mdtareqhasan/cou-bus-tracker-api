package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.MessageResponse;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/forgot-password-email")
@RequiredArgsConstructor
@Tag(name = "Password Reset Email", description = "Forgot password with email OTP verification")
public class PasswordResetEmailController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/init")
    @Operation(summary = "Send OTP for email password reset")
    public ResponseEntity<MessageResponse> sendResetOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String role = request.get("role");

        if (email == null || email.isBlank()) {
            throw new ApiException(ErrorCode.EMAIL_REQUIRED);
        }
        if (role == null || role.isBlank()) {
            throw new ApiException(ErrorCode.ROLE_REQUIRED, "Select a role (STUDENT or EMPLOYEE)");
        }

        passwordResetService.sendResetEmailOtp(email, role);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("OTP sent. Please check your email.")
                .code("OTP_SENT")
                .build());
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify email OTP and reset password")
    public ResponseEntity<MessageResponse> verifyAndReset(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String role = request.get("role");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        if (email == null || email.isBlank()) {
            throw new ApiException(ErrorCode.EMAIL_REQUIRED);
        }
        if (role == null || role.isBlank()) {
            throw new ApiException(ErrorCode.ROLE_REQUIRED);
        }
        if (otp == null || otp.isBlank()) {
            throw new ApiException(ErrorCode.OTP_REQUIRED);
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ApiException(ErrorCode.NEW_PASSWORD_REQUIRED);
        }

        passwordResetService.verifyAndResetEmailPassword(email, role, otp, newPassword);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Password reset successfully. Log in with your new password.")
                .code("PASSWORD_RESET_SUCCESS")
                .build());
    }
}
