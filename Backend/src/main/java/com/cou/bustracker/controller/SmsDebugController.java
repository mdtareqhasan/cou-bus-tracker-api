package com.cou.bustracker.controller;

import com.cou.bustracker.service.SmsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Tag(name = "Debug", description = "Debug endpoints for SMS gateway")
public class SmsDebugController {

    private final SmsService smsService;

    @GetMapping("/sms-balance")
    public ResponseEntity<Map<String, String>> getBalance() {
        String balance = smsService.getBalance();
        return ResponseEntity.ok(Map.of("balanceResponse", balance != null ? balance : "null"));
    }

    @GetMapping("/sms-test")
    public ResponseEntity<Map<String, String>> testSms(@RequestParam String phone) {
        boolean sent = smsService.sendOtpSms(phone, "123456");
        return ResponseEntity.ok(Map.of(
                "phone", phone,
                "sent", String.valueOf(sent),
                "message", sent ? "Test SMS submitted (check phone)" : "Failed - check server logs for BulkSMSBD Response"
        ));
    }
}
