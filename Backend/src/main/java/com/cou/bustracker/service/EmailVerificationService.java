package com.cou.bustracker.service;

import com.cou.bustracker.entity.EmailVerificationOtp;
import com.cou.bustracker.entity.EmailVerificationOtp.UserRole;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.repository.EmailVerificationOtpRepository;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.TeacherRepository;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private final EmailVerificationOtpRepository otpRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.email-verification.from}") private String from;
    @Value("${app.email-verification.from-name}") private String fromName;
    @Value("${app.email-verification.otp-expiry-minutes}") private long expiryMinutes;
    @Value("${app.email-verification.resend-cooldown-seconds}") private long resendCooldownSeconds;

    @Transactional
    public void sendOtp(String rawEmail, UserRole role, boolean isResend) {
        String email = normalize(rawEmail);
        ensureUnverifiedUserExists(email, role);
        LocalDateTime now = LocalDateTime.now();
        EmailVerificationOtp existing = otpRepository.findByEmailAndUserRole(email, role).orElse(null);
        if (isResend && existing != null && existing.getLastSentAt().plusSeconds(resendCooldownSeconds).isAfter(now)) {
            throw new IllegalArgumentException("Please wait before requesting another OTP");
        }

        String otp = "%06d".formatted(secureRandom.nextInt(1_000_000));
        EmailVerificationOtp record = existing == null ? EmailVerificationOtp.builder()
                .email(email).userRole(role).createdAt(now).build() : existing;
        record.setOtpHash(passwordEncoder.encode(otp));
        record.setExpiresAt(now.plusMinutes(expiryMinutes));
        record.setLastSentAt(now);
        record.setFailedAttempts(0);
        otpRepository.save(record);
        sendEmail(email, otp);
    }

    @Transactional
    public AuthResponse verifyOtp(String rawEmail, UserRole role, String otp) {
        String email = normalize(rawEmail);
        EmailVerificationOtp record = otpRepository.findByEmailAndUserRole(email, role)
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
        return role == UserRole.STUDENT ? verifyStudent(email) : verifyTeacher(email);
    }

    private AuthResponse verifyStudent(String email) {
        Student user = studentRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        user.setIsEmailVerified(true);
        // A correct OTP is the automatic approval step. Admins can still
        // deactivate or delete an account later if the ID card is invalid.
        user.setIsVerified(true);
        studentRepository.save(user);
        return authResponse(user.getId(), user.getName(), user.getEmail(), user.getIsVerified(), user.getIsEduMail(), "STUDENT");
    }

    private AuthResponse verifyTeacher(String email) {
        Teacher user = teacherRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        user.setIsEmailVerified(true);
        user.setIsVerified(true);
        teacherRepository.save(user);
        return authResponse(user.getId(), user.getName(), user.getEmail(), user.getIsVerified(), user.getIsEduMail(), "TEACHER");
    }

    private AuthResponse authResponse(Long id, String name, String email, Boolean verified, Boolean eduMail, String role) {
        return AuthResponse.builder().accessToken(jwtService.generateToken(email, role)).tokenType("Bearer")
                .role(role).id(id).name(name).email(email).isVerified(verified)
                .isEmailVerified(true).isEduMail(eduMail).build();
    }

    private void ensureUnverifiedUserExists(String email, UserRole role) {
        boolean verified;
        if (role == UserRole.STUDENT) {
            verified = studentRepository.findByEmail(email).map(Student::getIsEmailVerified)
                    .orElseThrow(() -> new IllegalArgumentException("Student registration not found"));
        } else {
            verified = teacherRepository.findByEmail(email).map(Teacher::getIsEmailVerified)
                    .orElseThrow(() -> new IllegalArgumentException("Teacher registration not found"));
        }
        if (verified) throw new IllegalArgumentException("Email is already verified");
    }

    private void sendEmail(String recipient, String otp) {
        if (from == null || from.isBlank()) throw new IllegalStateException("MAILERSEND_FROM_EMAIL is not configured");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromName + " <" + from + ">");
        message.setTo(recipient);
        message.setSubject("Your CoU Bus Tracker verification code");
        message.setText("Your email verification code is: " + otp + "\n\nThis code expires in "
                + expiryMinutes + " minutes. Do not share it with anyone.");
        mailSender.send(message);
    }

    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
