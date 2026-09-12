package com.cou.bustracker.controller;

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

@RestController
@RequestMapping("/api/auth/phone-verification")
@RequiredArgsConstructor
@Tag(name = "Phone Verification", description = "Phone OTP send, verify, and resend endpoints")
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/send")
    @Operation(summary = "Send OTP to phone number")
    public ResponseEntity<MessageResponse> sendOtp(@Valid @RequestBody SendPhoneOtpRequest request) {
        phoneVerificationService.sendOtp(request.phone(), request.role(), false);
        return ResponseEntity.ok(new MessageResponse("OTP sent successfully to " + request.phone()));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify phone OTP and return JWT")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyPhoneOtpRequest request) {
        return ResponseEntity.ok(phoneVerificationService.verifyOtp(request.phone(), request.role(), request.otp()));
    }

    @PostMapping("/resend")
    @Operation(summary = "Resend OTP to phone number")
    public ResponseEntity<MessageResponse> resendOtp(@Valid @RequestBody SendPhoneOtpRequest request) {
        phoneVerificationService.sendOtp(request.phone(), request.role(), true);
        return ResponseEntity.ok(new MessageResponse("OTP resent successfully to " + request.phone()));
    }
}
