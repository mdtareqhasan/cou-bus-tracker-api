package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.MessageResponse;
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
            throw new IllegalArgumentException("ইমেইল দিন");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("রোল নির্বাচন করুন (STUDENT বা EMPLOYEE)");
        }

        passwordResetService.sendResetEmailOtp(email, role);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("OTP পাঠানো হয়েছে। আপনার ইমেইল চেক করুন।")
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
            throw new IllegalArgumentException("ইমেইল দিন");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("রোল নির্বাচন করুন");
        }
        if (otp == null || otp.isBlank()) {
            throw new IllegalArgumentException("OTP দিন");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("নতুন পাসওয়ার্ড দিন");
        }

        passwordResetService.verifyAndResetEmailPassword(email, role, otp, newPassword);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("পাসওয়ার্ড সফলভাবে রিসেট হয়েছে। এখন নতুন পাসওয়ার্ড দিয়ে লগইন করুন।")
                .build());
    }
}
