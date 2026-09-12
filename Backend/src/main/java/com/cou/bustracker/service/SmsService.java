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
     * @param phoneNumber Receiver phone number (e.g., 8801XXXXXXXXX)
     * @param otp The 6-digit OTP code
     * @return true if sent successfully (HTTP 202)
     */
    public boolean sendOtpSms(String phoneNumber, String otp) {
        try {
            String message = "Your CoU Bus Tracker OTP is " + otp + ". It expires in 2 minutes. Do not share it.";
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String encodedPhone = URLEncoder.encode(phoneNumber, StandardCharsets.UTF_8);

            String url = SMS_API_URL
                    + "?api_key=" + apiKey
                    + "&type=text"
                    + "&number=" + encodedPhone
                    + "&senderid=" + senderId
                    + "&message=" + encodedMessage;

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OTP SMS sent successfully to {}", phoneNumber);
                return true;
            } else {
                log.error("Failed to send OTP SMS to {}: HTTP {}", phoneNumber, response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending OTP SMS to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
}
