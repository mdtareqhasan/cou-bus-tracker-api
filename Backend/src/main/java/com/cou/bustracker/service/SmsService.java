package com.cou.bustracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class SmsService {

    private static final String SMS_API_URL = "http://bulksmsbd.net/api/smsapi";

    @Value("${app.sms.api-key}")
    private String apiKey;

    @Value("${app.sms.sender-id}")
    private String senderId;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send OTP SMS via BulkSMSBD API.
     * @param phoneNumber Receiver phone number (will be normalized to 8801XXXXXXXXX)
     * @param otp The 6-digit OTP code
     * @return true if sent successfully
     */
    public boolean sendOtpSms(String phoneNumber, String otp) {
        try {
            String normalizedPhone = normalizePhone(phoneNumber);
            String message = "Your CoU Bus Tracker OTP is " + otp + ". It expires in 2 minutes. Do not share it.";
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

            String url = SMS_API_URL
                    + "?api_key=" + apiKey
                    + "&type=text"
                    + "&number=" + normalizedPhone
                    + "&senderid=" + senderId
                    + "&message=" + encodedMessage;

            log.info("Sending OTP SMS to {} (normalized: {})", phoneNumber, normalizedPhone);
            log.debug("SMS API URL: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);

            String responseBody = response.getBody();
            log.info("BulkSMSBD Response: HTTP {} | Body: {}", response.getStatusCode(), responseBody);

            // Check response body for BulkSMSBD error codes
            if (responseBody != null) {
                // BulkSMSBD returns error codes like 1001, 1002, etc.
                if (responseBody.contains("1001")) {
                    log.error("BulkSMSBD Error: Invalid Number");
                    return false;
                }
                if (responseBody.contains("1002")) {
                    log.error("BulkSMSBD Error: Sender ID not correct or disabled");
                    return false;
                }
                if (responseBody.contains("1007")) {
                    log.error("BulkSMSBD Error: Balance insufficient");
                    return false;
                }
                if (responseBody.contains("1032")) {
                    log.error("BulkSMSBD Error: IP not whitelisted");
                    return false;
                }
                if (responseBody.contains("202")) {
                    log.info("BulkSMSBD: SMS Submitted Successfully");
                    return true;
                }
            }

            // If HTTP 2xx, assume success
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OTP SMS sent successfully to {}", normalizedPhone);
                return true;
            } else {
                log.error("Failed to send OTP SMS to {}: HTTP {}", normalizedPhone, response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending OTP SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Normalize phone number to 8801XXXXXXXXX format.
     * Input: 01XXXXXXXXX or 8801XXXXXXXXX or +8801XXXXXXXXX
     * Output: 8801XXXXXXXXX
     */
    private String normalizePhone(String phone) {
        if (phone == null) return phone;
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("880") && cleaned.length() == 13) {
            return cleaned;
        }
        if (cleaned.startsWith("01") && cleaned.length() == 11) {
            return "88" + cleaned;
        }
        return cleaned;
    }
}
