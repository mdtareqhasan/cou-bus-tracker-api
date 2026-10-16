package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.entity.EmailVerificationOtp.UserRole;
import com.cou.bustracker.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EmailVerificationControllerTest {

    private MockMvc mockMvc;
    private StubEmailVerificationService stubService;

    @BeforeEach
    void setUp() {
        stubService = new StubEmailVerificationService();
        mockMvc = MockMvcBuilders.standaloneSetup(new EmailVerificationController(stubService)).build();
    }

    @Test
    void initRegistration_shouldReturnOk() throws Exception {
        String payload = """
                {
                  "role": "STUDENT",
                  "name": "Test Student",
                  "email": "student@example.com",
                  "password": "secret123",
                  "department": "CSE",
                  "rollNumber": "1607041",
                  "session": "2016-17"
                }
                """;

        mockMvc.perform(post("/api/auth/email-verification/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent successfully to student@example.com"));

        assertEquals(true, stubService.initCalled);
    }

    @Test
    void initRegistration_shouldRejectInvalidEmail() throws Exception {
        String payload = """
                {
                  "role": "STUDENT",
                  "name": "Test Student",
                  "email": "not-an-email",
                  "password": "secret123",
                  "department": "CSE",
                  "rollNumber": "1607041",
                  "session": "2016-17"
                }
                """;

        mockMvc.perform(post("/api/auth/email-verification/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyOtp_shouldReturnAuthResponse() throws Exception {
        mockMvc.perform(post("/api/auth/email-verification/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"role\":\"STUDENT\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("dummy-token"))
                .andExpect(jsonPath("$.email").value("student@example.com"));
    }

    @Test
    void verifyOtp_shouldRejectInvalidOtpFormat() throws Exception {
        mockMvc.perform(post("/api/auth/email-verification/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"role\":\"STUDENT\",\"otp\":\"12\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendOtp_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/auth/email-verification/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP resent successfully to student@example.com"));

        assertEquals("student@example.com", stubService.lastEmail);
        assertEquals(UserRole.STUDENT, stubService.lastRole);
        assertEquals(true, stubService.lastIsResend);
    }

    @Test
    void sendOtp_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/auth/email-verification/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"employee@example.com\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("OTP sent successfully")));

        assertEquals("employee@example.com", stubService.lastEmail);
        assertEquals(UserRole.EMPLOYEE, stubService.lastRole);
        assertEquals(false, stubService.lastIsResend);
    }

    static class StubEmailVerificationService extends EmailVerificationService {
        String lastEmail;
        UserRole lastRole;
        boolean lastIsResend;
        boolean initCalled;

        StubEmailVerificationService() {
            super(null, null, null, null);
        }

        @Override
        public void initRegistration(com.cou.bustracker.dto.request.EmailVerificationInitRequest request) {
            initCalled = true;
        }

        @Override
        public void sendOtp(String rawEmail, UserRole role, boolean isResend) {
            lastEmail = rawEmail;
            lastRole = role;
            lastIsResend = isResend;
        }

        @Override
        public AuthResponse verifyOtp(String rawEmail, UserRole role, String otp) {
            return AuthResponse.builder()
                    .accessToken("dummy-token")
                    .tokenType("Bearer")
                    .role(role.name())
                    .id(1L)
                    .name("Test Student")
                    .email(rawEmail)
                    .isVerified(true)
                    .build();
        }
    }
}
