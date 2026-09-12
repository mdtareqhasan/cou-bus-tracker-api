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

    private static final String SMS_API_URL = "https://bulksmsbd.net/api/smsapi";
    private static final String BALANCE_API_URL = "https://bulksmsbd.net/api/getBalanceApi";

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
            log.debug("SMS API URL: {}", url.replace(apiKey, "****"));

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);

            String responseBody = response.getBody();
            log.info("BulkSMSBD Response: HTTP {} | Body: {}", response.getStatusCode(), responseBody);

            // Check response body for BulkSMSBD error codes - https://bulksmsbd.net/developers
            if (responseBody != null) {
                String body = responseBody.trim();
                if (body.contains("1001")) {
                    log.error("BulkSMSBD Error 1001: Invalid Number [{}]", body);
                    return false;
                }
                if (body.contains("1002")) {
                    log.error("BulkSMSBD Error 1002: Sender ID not correct or disabled [{}]", body);
                    return false;
                }
                if (body.contains("1003")) {
                    log.error("BulkSMSBD Error 1003: Please provide all required fields [{}]", body);
                    return false;
                }
                if (body.contains("1005") || body.contains("Internal Error")) {
                    log.error("BulkSMSBD Error 1005: Internal Error [{}]", body);
                    return false;
                }
                if (body.contains("1006")) {
                    log.error("BulkSMSBD Error 1006: Balance Validity Not Available [{}]", body);
                    return false;
                }
                if (body.contains("1007")) {
                    log.error("BulkSMSBD Error 1007: Balance Insufficient [{}]", body);
                    return false;
                }
                if (body.contains("1011")) {
                    log.error("BulkSMSBD Error 1011: User ID not found [{}]", body);
                    return false;
                }
                if (body.contains("1012")) {
                    log.error("BulkSMSBD Error 1012: Masking SMS must be sent in Bengali [{}]", body);
                    return false;
                }
                if (body.contains("1013")) {
                    log.error("BulkSMSBD Error 1013: Sender ID has not found Gateway by api key [{}] - API key and Sender ID mismatch!", body);
                    return false;
                }
                if (body.contains("1032")) {
                    log.error("BulkSMSBD Error 1032: IP not whitelisted [{}] - Disable IP whitelist in BulkSMSBD dashboard", body);
                    return false;
                }
                if (body.contains("202")) {
                    log.info("BulkSMSBD: SMS Submitted Successfully [{}]", body);
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
     * Check BulkSMSBD account balance - useful for debugging.
     * Mirrors PHP get_balance() from https://bulksmsbd.net/developers
     * POST https://bulksmsbd.net/api/getBalanceApi with api_key
     */
    public String getBalance() {
        try {
            log.info("Checking BulkSMSBD balance for api_key: {}...", apiKey.substring(0, Math.min(4, apiKey.length())) + "****");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String body = "api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(BALANCE_API_URL, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();
            log.info("BulkSMSBD Balance Response: HTTP {} | Body: {}", response.getStatusCode(), responseBody);
            return responseBody;
        } catch (Exception e) {
            log.error("Error checking BulkSMSBD balance: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Normalize phone number to 8801XXXXXXXXX format for BulkSMSBD API.
     * Input: 01XXXXXXXXX (11 digit BD format)
     * Output: 8801XXXXXXXXX (13 digit with country code)
     */
    private String normalizePhone(String phone) {
        if (phone == null) return phone;
        String cleaned = phone.replaceAll("[^0-9]", "");
        // BD numbers start with 01 - add 88 country code
        if (cleaned.startsWith("01") && cleaned.length() == 11) {
            return "88" + cleaned;
        }
        if (cleaned.startsWith("880") && cleaned.length() == 13) {
            return cleaned;
        }
        return cleaned;
    }
}
