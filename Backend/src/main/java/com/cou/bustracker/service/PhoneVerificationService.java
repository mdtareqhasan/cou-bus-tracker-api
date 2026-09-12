package com.cou.bustracker.service;

import com.cou.bustracker.entity.PhoneVerificationOtp;
import com.cou.bustracker.entity.PhoneVerificationOtp.UserRole;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.repository.PhoneVerificationOtpRepository;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.TeacherRepository;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneVerificationService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private final PhoneVerificationOtpRepository otpRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.phone-verification.otp-expiry-minutes}") private long expiryMinutes;
    @Value("${app.phone-verification.resend-cooldown-seconds}") private long resendCooldownSeconds;

    @Transactional
    public void sendOtp(String rawPhone, UserRole role, boolean isResend) {
        String phone = normalize(rawPhone);
        LocalDateTime now = LocalDateTime.now();
        PhoneVerificationOtp existing = otpRepository.findByPhoneAndUserRole(phone, role).orElse(null);
        if (isResend && existing != null && existing.getLastSentAt().plusSeconds(resendCooldownSeconds).isAfter(now)) {
            throw new IllegalArgumentException("Please wait before requesting another OTP");
        }

        String otp = "%06d".formatted(secureRandom.nextInt(1_000_000));
        PhoneVerificationOtp record = existing == null ? PhoneVerificationOtp.builder()
                .phone(phone).userRole(role).createdAt(now).build() : existing;
        record.setOtpHash(passwordEncoder.encode(otp));
        record.setExpiresAt(now.plusMinutes(expiryMinutes));
        record.setLastSentAt(now);
        record.setFailedAttempts(0);
        otpRepository.save(record);
        log.info("=========================================================");
        log.info("OTP for phone={} role={} -> [DEV ONLY] OTP CODE: {} (expires in {} min)", phone, role, otp, expiryMinutes);
        log.info("=========================================================");
        boolean sent = smsService.sendOtpSms(phone, otp);
        if (!sent) {
            log.error("Failed to send OTP SMS to {} - check BulkSMSBD Response logs above (balance/senderId/IP)", phone);
            throw new IllegalStateException("Failed to send OTP SMS. Please check SMS gateway (balance/sender ID/IP whitelist) or try again. Check server logs for BulkSMSBD Response.");
        }
        log.info("OTP generated and SMS sent successfully to {} (expires in {} min)", phone, expiryMinutes);
    }

    @Transactional
    public AuthResponse verifyOtp(String rawPhone, UserRole role, String otp) {
        String phone = normalize(rawPhone);
        PhoneVerificationOtp record = otpRepository.findByPhoneAndUserRole(phone, role)
                .orElseThrow(() -> new IllegalArgumentException("No OTP found. Please request a new OTP"));
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpRepository.delete(record);
            throw new IllegalArgumentException("OTP has expired. Please request a new OTP");
        }
        if (record.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            otpRepository.delete(record);
            throw new IllegalArgumentException("Too many incorrect attempts. Please request a new OTP");
        }
        if (!passwordEncoder.matches(otp, record.getOtpHash())) {
            record.setFailedAttempts(record.getFailedAttempts() + 1);
            otpRepository.save(record);
            throw new BadCredentialsException("Invalid OTP");
        }

        otpRepository.delete(record);
        return role == UserRole.STUDENT ? verifyStudent(phone) : verifyTeacher(phone);
    }

    private AuthResponse verifyStudent(String phone) {
        Student user = studentRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with this phone number"));
        user.setIsPhoneVerified(true);
        user.setIsVerified(true);
        studentRepository.save(user);
        return authResponse(user.getId(), user.getName(), user.getPhone(), user.getIsVerified(), "STUDENT");
    }

    private AuthResponse verifyTeacher(String phone) {
        Teacher user = teacherRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with this phone number"));
        user.setIsPhoneVerified(true);
        user.setIsVerified(true);
        teacherRepository.save(user);
        return authResponse(user.getId(), user.getName(), user.getPhone(), user.getIsVerified(), "TEACHER");
    }

    private AuthResponse authResponse(Long id, String name, String phone, Boolean verified, String role) {
        return AuthResponse.builder().accessToken(jwtService.generateToken(phone, role)).tokenType("Bearer")
                .role(role).id(id).name(name).phone(phone).isVerified(verified)
                .isPhoneVerified(true).build();
    }

    private String normalize(String phone) {
        if (phone == null) return null;
        String cleaned = phone.trim().replaceAll("[^0-9]", "");
        // Store as 11-digit BD format: 01XXXXXXXXX
        if (cleaned.startsWith("880") && cleaned.length() == 13) {
            return cleaned.substring(2);
        }
        if (cleaned.startsWith("01") && cleaned.length() == 11) {
            return cleaned;
        }
        return cleaned;
    }
}
