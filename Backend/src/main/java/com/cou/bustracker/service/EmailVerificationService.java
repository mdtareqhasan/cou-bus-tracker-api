package com.cou.bustracker.service;

import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.entity.EmailVerificationOtp.UserRole;
import org.springframework.stereotype.Service;

/**
 * Deprecated: Email verification removed. Phone verification is used instead.
 * Kept only to satisfy dependency injection for legacy controllers.
 */
@Service
public class EmailVerificationService {

    public void sendOtp(String rawEmail, UserRole role, boolean isResend) {
        throw new UnsupportedOperationException("Email verification is deprecated. Use phone verification (/api/auth/phone-verification/**) instead.");
    }

    public AuthResponse verifyOtp(String rawEmail, UserRole role, String otp) {
        throw new UnsupportedOperationException("Email verification is deprecated. Use phone verification instead.");
    }
}
