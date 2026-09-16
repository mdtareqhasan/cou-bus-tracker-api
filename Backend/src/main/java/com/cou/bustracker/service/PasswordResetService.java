package com.cou.bustracker.service;

import com.cou.bustracker.entity.PhoneVerificationOtp.UserRole;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.repository.PhoneVerificationOtpRepository;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.TeacherRepository;
import com.cou.bustracker.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PhoneVerificationOtpRepository otpRepository;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.phone-verification.otp-expiry-minutes:2}")
    private long expiryMinutes;

    @Value("${app.sms.allow-soft-fail:true}")
    private boolean allowSoftFail;

    /**
     * Step 1: Send OTP for password reset.
     * Validates that the phone exists in the appropriate table.
     */
    public void sendResetOtp(String rawPhone, String role) {
        String phone = PhoneUtils.normalizeBd(rawPhone);
        UserRole userRole = parseRole(role);

        // Check if user exists
        if (userRole == UserRole.STUDENT) {
            if (!studentRepository.existsByPhone(phone)) {
                throw new IllegalArgumentException("এই ফোন নম্বর দিয়ে কোনো শিক্ষার্থী পাওয়া যায়নি।");
            }
        } else {
            if (!teacherRepository.existsByPhone(phone)) {
                throw new IllegalArgumentException("এই ফোন নম্বর দিয়ে কোনো শিক্ষক পাওয়া যায়নি।");
            }
        }

        // Generate and send OTP (reuse the phone_verification_otps table)
        String otp = "%06d".formatted(secureRandom.nextInt(1_000_000));
        var existing = otpRepository.findByPhoneAndUserRole(phone, userRole).orElse(null);

        var record = existing == null
                ? com.cou.bustracker.entity.PhoneVerificationOtp.builder()
                        .phone(phone)
                        .userRole(userRole)
                        .createdAt(LocalDateTime.now())
                        .build()
                : existing;

        record.setOtpHash(passwordEncoder.encode(otp));
        record.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        record.setLastSentAt(LocalDateTime.now());
        record.setFailedAttempts(0);
        // Clear any pending registration data since this is a password reset
        record.setPendingRegistrationJson(null);
        record.setPendingIdCardUrl(null);
        otpRepository.save(record);

        log.info("Password reset OTP for phone={} role={} -> [DEV] OTP: {}", phone, userRole, otp);

        boolean sent = smsService.sendOtpSms(phone, otp);
        if (!sent && !allowSoftFail) {
            throw new IllegalStateException("Failed to send OTP SMS. Please try again.");
        }

        log.info("Password reset OTP sent to {} (expires in {} min)", phone, expiryMinutes);
    }

    /**
     * Step 2: Verify OTP and reset password.
     */
    @Transactional
    public void verifyAndResetPassword(String rawPhone, String role, String otp, String newPassword) {
        String phone = PhoneUtils.normalizeBd(rawPhone);
        UserRole userRole = parseRole(role);

        var record = otpRepository.findByPhoneAndUserRole(phone, userRole)
                .orElseThrow(() -> new IllegalArgumentException("OTP পাওয়া যায়নি। নতুন করে অনুরোধ করুন।"));

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpRepository.delete(record);
            throw new IllegalArgumentException("OTP মেয়াদ শেষ হয়েছে। নতুন করে অনুরোধ করুন।");
        }

        if (record.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            otpRepository.delete(record);
            throw new IllegalArgumentException("অনেক বার ভুল চেষ্টা হয়েছে। নতুন করে অনুরোধ করুন।");
        }

        if (!passwordEncoder.matches(otp, record.getOtpHash())) {
            record.setFailedAttempts(record.getFailedAttempts() + 1);
            otpRepository.save(record);
            throw new BadCredentialsException("ভুল OTP। আবার চেষ্টা করুন।");
        }

        // OTP verified — reset password
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("নতুন পাসওয়ার্ড প্রয়োজন।");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);

        if (userRole == UserRole.STUDENT) {
            Student student = studentRepository.findByPhone(phone)
                    .orElseThrow(() -> new IllegalArgumentException("শিক্ষার্থী পাওয়া যায়নি।"));
            student.setPassword(encodedPassword);
            studentRepository.save(student);
        } else {
            Teacher teacher = teacherRepository.findByPhone(phone)
                    .orElseThrow(() -> new IllegalArgumentException("শিক্ষক পাওয়া যায়নি।"));
            teacher.setPassword(encodedPassword);
            teacherRepository.save(teacher);
        }

        // Delete OTP record after successful reset
        otpRepository.delete(record);

        log.info("Password reset successful for phone={} role={}", phone, userRole);
    }

    private UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role. Must be STUDENT or TEACHER");
        }
    }
}
