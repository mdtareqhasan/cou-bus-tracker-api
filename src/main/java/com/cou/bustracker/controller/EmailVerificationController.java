package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.EmailVerificationRequest;
import com.cou.bustracker.dto.request.VerifyEmailOtpRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.MessageResponse;
import com.cou.bustracker.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email-verification")
@RequiredArgsConstructor
@Tag(name = "Email Verification", description = "Registration email OTP endpoints")
public class EmailVerificationController {
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/verify")
    @Operation(summary = "Verify the six-digit registration OTP")
    public ResponseEntity<AuthResponse> verify(@Valid @RequestBody VerifyEmailOtpRequest request) {
        return ResponseEntity.ok(emailVerificationService.verifyOtp(request.email(), request.role(), request.otp()));
    }

    @PostMapping("/resend")
    @Operation(summary = "Resend registration OTP; limited to one request per minute")
    public ResponseEntity<MessageResponse> resend(@Valid @RequestBody EmailVerificationRequest request) {
        emailVerificationService.sendOtp(request.email(), request.role(), true);
        return ResponseEntity.ok(MessageResponse.builder().message("A new OTP has been sent to your email").build());
    }
}
