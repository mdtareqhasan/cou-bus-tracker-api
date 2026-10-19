package com.cou.bustracker.service;

import com.cou.bustracker.entity.EmailVerificationOtp;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.repository.EmailVerificationOtpRepository;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Forgot-password flow, e-mail only.
 *
 * <p>The earlier phone/SMS variant was removed along with the rest of the
 * phone vertical: SMS registration is dead (the app is Google-only, and
 * e-mail registration never collected a phone), so no account can hold a
 * phone number to reset against.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final EmailVerificationOtpRepository emailOtpRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.email-verification.otp-expiry-minutes:10}")
    private long expiryMinutes;

    public void sendResetEmailOtp(String rawEmail, String role) {
        String email = rawEmail.trim().toLowerCase();
        EmailVerificationOtp.UserRole emailRole = parseEmailRole(role);

        if (emailRole == EmailVerificationOtp.UserRole.STUDENT) {
            if (!studentRepository.existsByEmail(email)) {
                throw new ApiException(ErrorCode.NO_STUDENT_WITH_EMAIL);
            }
        } else {
            if (!teacherRepository.existsByEmail(email)) {
                throw new ApiException(ErrorCode.NO_TEACHER_WITH_EMAIL);
            }
        }

        String otp = "%06d".formatted(secureRandom.nextInt(1_000_000));
        var existing = emailOtpRepository.findByEmailAndUserRole(email, emailRole).orElse(null);

        var record = existing == null
                ? com.cou.bustracker.entity.EmailVerificationOtp.builder()
                        .email(email)
                        .userRole(emailRole)
                        .failedAttempts(0)
                        .createdAt(LocalDateTime.now())
                        .build()
                : existing;

        record.setOtpHash(passwordEncoder.encode(otp));
        record.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        record.setLastSentAt(LocalDateTime.now());
        record.setFailedAttempts(0);
        emailOtpRepository.save(record);

        // Placeholder: wire email sending provider later.
        log.info("Password reset email OTP for email={} role={} -> [DEV] OTP: {}", email, emailRole, otp);
    }

    @Transactional
    public void verifyAndResetEmailPassword(String rawEmail, String role, String otp, String newPassword) {
        String email = rawEmail.trim().toLowerCase();
        EmailVerificationOtp.UserRole emailRole = parseEmailRole(role);

        var record = emailOtpRepository.findByEmailAndUserRole(email, emailRole)
                .orElseThrow(() -> new ApiException(ErrorCode.OTP_NOT_FOUND));

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            emailOtpRepository.delete(record);
            throw new ApiException(ErrorCode.OTP_EXPIRED);
        }

        if (record.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            emailOtpRepository.delete(record);
            throw new ApiException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
        }

        if (!passwordEncoder.matches(otp, record.getOtpHash())) {
            record.setFailedAttempts(record.getFailedAttempts() + 1);
            emailOtpRepository.save(record);
            throw new ApiException(ErrorCode.OTP_INVALID);
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new ApiException(ErrorCode.NEW_PASSWORD_REQUIRED);
        }
        if (newPassword.length() < 6) {
            throw new ApiException(ErrorCode.PASSWORD_TOO_SHORT);
        }

        String encodedPassword = passwordEncoder.encode(newPassword);

        if (emailRole == EmailVerificationOtp.UserRole.STUDENT) {
            Student student = studentRepository.findByEmail(email)
                    .orElseThrow(() -> new ApiException(ErrorCode.STUDENT_NOT_FOUND));
            student.setPassword(encodedPassword);
            studentRepository.save(student);
        } else {
            Teacher teacher = teacherRepository.findByEmail(email)
                    .orElseThrow(() -> new ApiException(ErrorCode.TEACHER_NOT_FOUND));
            teacher.setPassword(encodedPassword);
            teacherRepository.save(teacher);
        }

        emailOtpRepository.delete(record);
        log.info("Password reset successful for email={} role={}", email, emailRole);
    }

    private EmailVerificationOtp.UserRole parseEmailRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ApiException(ErrorCode.ROLE_REQUIRED);
        }
        try {
            return EmailVerificationOtp.UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_ROLE, "Invalid role. Must be STUDENT or EMPLOYEE");
        }
    }
}
