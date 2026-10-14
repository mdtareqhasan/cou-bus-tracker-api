package com.cou.bustracker.controller;

import com.cou.bustracker.entity.Student;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.service.SmsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Tag(name = "Debug", description = "Debug endpoints for SMS gateway and auth")
@Slf4j
public class SmsDebugController {

    private final SmsService smsService;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

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
                ? "Sender ID longer than 11 chars - BulkSMSBD typically requires <=11 for non-masking."
                : "OK");
        result.put("balanceResponse", smsService.getBalance());
        return ResponseEntity.ok(result);
    }

    /**
     * Debug login: test password match directly.
     * GET /api/debug/test-login?phone=01793165308&password=student123
     */
    @GetMapping("/test-login")
    public ResponseEntity<Map<String, Object>> testLogin(@RequestParam String phone, @RequestParam String password) {
        Map<String, Object> result = new LinkedHashMap<>();
        // Normalize phone same as StudentService
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("880") && cleaned.length() == 13) cleaned = cleaned.substring(2);
        if (cleaned.length() == 10) cleaned = "0" + cleaned;
        result.put("normalizedPhone", cleaned);

        Student student = studentRepository.findByPhone(cleaned).orElse(null);
        if (student == null) {
            result.put("found", false);
            result.put("error", "Student not found with phone: " + cleaned);
            // Try to find with any phone format
            result.put("hint", "Check if phone in DB matches. Run: SELECT id, name, phone, is_phone_verified, is_verified FROM students");
            return ResponseEntity.ok(result);
        }
        result.put("found", true);
        result.put("studentId", student.getId());
        result.put("name", student.getName());
        result.put("phone", student.getPhone());
        result.put("phoneFormatMatch", student.getPhone().equals(cleaned));
        result.put("isPhoneVerified", student.getIsPhoneVerified());
        result.put("isVerified", student.getIsVerified());
        result.put("isActive", student.getIsActive());
        result.put("passwordIsNull", student.getPassword() == null);
        result.put("passwordLength", student.getPassword() != null ? student.getPassword().length() : 0);
        result.put("passwordPrefix", student.getPassword() != null && student.getPassword().length() > 10
                ? student.getPassword().substring(0, 10) + "..." : student.getPassword());

        if (student.getPassword() != null) {
            boolean matches = passwordEncoder.matches(password, student.getPassword());
            result.put("passwordMatch", matches);
        } else {
            result.put("passwordMatch", false);
            result.put("error", "Password is null in DB!");
        }
        return ResponseEntity.ok(result);
    }
}
