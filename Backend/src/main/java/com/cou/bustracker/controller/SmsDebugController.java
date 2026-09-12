package com.cou.bustracker.controller;

import com.cou.bustracker.service.SmsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Tag(name = "Debug", description = "Debug endpoints for SMS gateway")
public class SmsDebugController {

    private final SmsService smsService;

    @Value("${app.sms.api-key}")
    private String apiKey;

    @Value("${app.sms.sender-id}")
    private String senderId;

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

    /**
     * Diagnostic endpoint: shows current SMS configuration (masked) and runs a balance check.
     * Visit: http://localhost:8080/api/debug/sms-diag
     */
    @GetMapping("/sms-diag")
    public ResponseEntity<Map<String, Object>> diagnose() {
        Map<String, Object> result = new LinkedHashMap<>();
        String maskedKey = apiKey == null || apiKey.length() < 4
                ? "null"
                : apiKey.substring(0, 4) + "****";
        result.put("apiKey", maskedKey);
        result.put("apiKeyLength", apiKey == null ? 0 : apiKey.length());
        result.put("senderId", senderId);
        result.put("senderIdLength", senderId == null ? 0 : senderId.length());
        result.put("senderIdWarning", senderId != null && senderId.length() > 11
                ? "Sender ID longer than 11 chars - BulkSMSBD typically requires <=11 for non-masking. If using masking, ensure it is Approved in dashboard."
                : "OK");
        result.put("balanceResponse", smsService.getBalance());
        return ResponseEntity.ok(result);
    }
}
