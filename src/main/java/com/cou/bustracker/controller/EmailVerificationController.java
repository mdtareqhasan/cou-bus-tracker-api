package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.EmailVerificationInitRequest;
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
@Tag(name = "Email Verification", description = "Email OTP send, verify, and resend endpoints")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/init")
    @Operation(summary = "Initialize OTP-first registration (validates payload + sends email OTP)")
    public ResponseEntity<MessageResponse> initRegistration(
            @Valid @RequestBody EmailVerificationInitRequest request) {
        emailVerificationService.initRegistration(request);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("OTP sent successfully to " + request.getEmail())
                .build());
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify email OTP — creates the user on success and returns JWT")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        return ResponseEntity.ok(emailVerificationService.verifyOtp(
                request.email(), request.role(), request.otp()));
    }

    @PostMapping("/resend")
    @Operation(summary = "Resend OTP to an email with a pending registration")
    public ResponseEntity<MessageResponse> resendOtp(@Valid @RequestBody EmailVerificationRequest request) {
        emailVerificationService.sendOtp(request.email(), request.role(), true);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("OTP resent successfully to " + request.email())
                .build());
    }

    @PostMapping("/send")
    @Operation(summary = "Send OTP — legacy; prefer /init for new registrations")
    public ResponseEntity<MessageResponse> sendOtp(@Valid @RequestBody EmailVerificationRequest request) {
        emailVerificationService.sendOtp(request.email(), request.role(), false);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("OTP sent successfully to " + request.email())
                .build());
    }
}
