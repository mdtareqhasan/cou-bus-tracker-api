package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.PhoneVerificationInitRequest;
import com.cou.bustracker.dto.request.SendPhoneOtpRequest;
import com.cou.bustracker.dto.request.VerifyPhoneOtpRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.MessageResponse;
import com.cou.bustracker.service.PhoneVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth/phone-verification")
@RequiredArgsConstructor
@Tag(name = "Phone Verification", description = "Phone OTP send, verify, and resend endpoints")
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    /**
     * OTP-first registration entry point. Accepts the full Student/Teacher
     * payload (multipart/form-data) + the ID-card image. Validates and
     * uploads the card, stages the data inside phone_verification_otps, and
     * sends the SMS OTP. NO Student/Teacher row is created until
     * {@link #verifyOtp} succeeds.
     */
    @PostMapping(value = "/init", consumes = {"multipart/form-data"})
    @Operation(summary = "Initialize OTP-first registration (validates + uploads ID card + sends OTP)")
    public ResponseEntity<MessageResponse> initRegistration(
            @Valid @ModelAttribute PhoneVerificationInitRequest request,
            @RequestParam("idCard") MultipartFile idCard) throws java.io.IOException {
        phoneVerificationService.initRegistration(request, idCard);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("OTP sent successfully to " + request.getPhone() +
                         ". Please verify within " + "2 minutes.")
                .build());
    }

    /**
     * Verify the OTP and (only on success) create the Student/Teacher row.
     */
    @PostMapping("/verify")
    @Operation(summary = "Verify phone OTP — creates the user on success and returns JWT")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyPhoneOtpRequest request) {
        return ResponseEntity.ok(phoneVerificationService.verifyOtp(
                request.phone(), request.role(), request.otp()));
    }

    /**
     * Resend an OTP to a phone that already has a pending registration. The
     * 60-second cooldown still applies.
     */
    @PostMapping("/resend")
    @Operation(summary = "Resend OTP to a phone with a pending registration")
    public ResponseEntity<MessageResponse> resendOtp(@Valid @RequestBody SendPhoneOtpRequest request) {
        phoneVerificationService.sendOtp(request.phone(), request.role(), true);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("OTP resent successfully to " + request.phone())
                .build());
    }

    /**
     * Legacy "send" endpoint. Prefer {@link #resendOtp} for callers already
     * in the registration flow, and {@link #initRegistration} for fresh ones.
     */
    @PostMapping("/send")
    @Operation(summary = "Send OTP — legacy; prefer /init for new registrations")
    public ResponseEntity<MessageResponse> sendOtp(@Valid @RequestBody SendPhoneOtpRequest request) {
        phoneVerificationService.sendOtp(request.phone(), request.role(), false);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("OTP sent successfully to " + request.phone())
                .build());
    }
}
