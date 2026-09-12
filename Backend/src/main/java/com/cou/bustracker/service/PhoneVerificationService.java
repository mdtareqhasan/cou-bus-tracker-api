package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.PhoneVerificationInitRequest;
import com.cou.bustracker.entity.PhoneVerificationOtp;
import com.cou.bustracker.entity.PhoneVerificationOtp.UserRole;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.repository.PhoneVerificationOtpRepository;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.TeacherRepository;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.security.JwtService;
import com.cou.bustracker.util.PhoneUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneVerificationService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    /** Grace period after expiry before the @Scheduled cleaner sweeps the row. */
    private static final long CLEANUP_GRACE_HOURS = 24L;
    private final PhoneVerificationOtpRepository otpRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final FileStorageService fileStorageService;
    private final CloudinaryService cloudinaryService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.phone-verification.otp-expiry-minutes}") private long expiryMinutes;
    @Value("${app.phone-verification.resend-cooldown-seconds}") private long resendCooldownSeconds;
    @Value("${app.sms.allow-soft-fail:true}") private boolean allowSoftFail;

    // =========================================================================
    // Stage 1 — initRegistration
    //
    // Validates the full Student/Teacher payload + ID card, uploads the card to
    // Cloudinary, and stages the registration inside phone_verification_otps.
    // NO Student/Teacher row is created at this point.
    // =========================================================================
    @Transactional
    public void initRegistration(PhoneVerificationInitRequest req, MultipartFile idCard) throws java.io.IOException {
        String phone = PhoneUtils.normalizeBd(req.getPhone());
        UserRole role = req.getRole();

        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        // ----- role-specific field + uniqueness validation (BEFORE sending OTP) -----
        if (role == UserRole.STUDENT) {
            if (req.getStudentId() == null || req.getStudentId().isBlank()) {
                throw new IllegalArgumentException("Student ID is required");
            }
            if (req.getVarsityBatch() == null || req.getVarsityBatch().isBlank()) {
                throw new IllegalArgumentException("Varsity batch is required");
            }
            if (studentRepository.existsByPhone(phone)) {
                throw new IllegalStateException("এই ফোন নম্বর ইতিমধ্যে ব্যবহৃত হয়েছে।");
            }
            if (studentRepository.existsByStudentId(req.getStudentId())) {
                throw new IllegalStateException("এই শিক্ষার্থী ID ইতিমধ্যে নিবন্ধিত।");
            }
        } else {
            if (req.getTeacherId() == null || req.getTeacherId().isBlank()) {
                throw new IllegalArgumentException("Teacher ID is required");
            }
            if (teacherRepository.existsByPhone(phone)) {
                throw new IllegalStateException("এই ফোন নম্বর ইতিমধ্যে ব্যবহৃত হয়েছে।");
            }
            if (req.getTeacherId() != null && !req.getTeacherId().isBlank()
                    && teacherRepository.existsByTeacherId(req.getTeacherId())) {
                throw new IllegalStateException("এই শিক্ষক ID ইতিমধ্যে নিবন্ধিত।");
            }
        }

        // Password is required unless using Google Sign-In
        boolean hasGoogle = req.getGoogleIdToken() != null && !req.getGoogleIdToken().isBlank();
        String passwordHash = null;
        if (!hasGoogle) {
            if (req.getPassword() == null || req.getPassword().isBlank()) {
                throw new IllegalArgumentException("Password is required");
            }
            if (req.getPassword().length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters");
            }
            passwordHash = passwordEncoder.encode(req.getPassword());
        }

        // ----- ID card upload (validates + uploads to Cloudinary) -----
        String subDir = role == UserRole.STUDENT ? "student-id-cards" : "teacher-id-cards";
        String idCardUrl = fileStorageService.storeIdCard(idCard, subDir);

        // ----- serialize payload to JSON (password is stored as a hash, never plain) -----
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", req.getName());
        payload.put("phone", phone);
        payload.put("passwordHash", passwordHash); // may be null when googleIdToken is set
        payload.put("googleIdToken", req.getGoogleIdToken());
        payload.put("department", req.getDepartment());
        if (role == UserRole.STUDENT) {
            payload.put("studentId", req.getStudentId());
            payload.put("varsityBatch", req.getVarsityBatch());
        } else {
            payload.put("teacherId", req.getTeacherId());
            payload.put("designation", req.getDesignation());
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            // Roll back the Cloudinary upload to avoid an orphan file
            safeDeleteCloudinary(idCardUrl);
            throw new IllegalStateException("Failed to serialize registration payload: " + e.getMessage(), e);
        }

        // ----- send OTP (also persists / overwrites the OTP row with payload + id-card URL) -----
        internalSendOtp(phone, role, json, idCardUrl, false);
    }

    // =========================================================================
    // Stage 2 — verifyOtp
    //
    // On successful OTP entry, creates the Student or Teacher row from the
    // staged payload and returns a JWT. If the OTP has no pending payload
    // (legacy / manually-inserted row), fall back to the old "verify existing
    // student" behaviour for backward compatibility.
    // =========================================================================
    @Transactional
    public AuthResponse verifyOtp(String rawPhone, UserRole role, String otp) {
        String phone = PhoneUtils.normalizeBd(rawPhone);
        PhoneVerificationOtp record = otpRepository.findByPhoneAndUserRole(phone, role)
                .orElseThrow(() -> new IllegalArgumentException("No OTP found. Please request a new OTP"));
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            cleanupSingleRecord(record);
            throw new IllegalArgumentException("OTP has expired. Please request a new OTP");
        }
        if (record.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            cleanupSingleRecord(record);
            throw new IllegalArgumentException("Too many incorrect attempts. Please request a new OTP");
        }
        if (!passwordEncoder.matches(otp, record.getOtpHash())) {
            record.setFailedAttempts(record.getFailedAttempts() + 1);
            otpRepository.save(record);
            throw new BadCredentialsException("Invalid OTP");
        }

        // OTP matched — decide path based on whether pending data exists.
        if (record.getPendingRegistrationJson() != null && !record.getPendingRegistrationJson().isBlank()) {
            String idCardUrl = record.getPendingIdCardUrl();
            String json = record.getPendingRegistrationJson();
            // Delete the OTP row first; the Student/Teacher creation is independent.
            otpRepository.delete(record);
            AuthResponse resp = role == UserRole.STUDENT
                    ? createStudentFromPending(json, idCardUrl)
                    : createTeacherFromPending(json, idCardUrl);
            log.info("OTP verified; new {} row created for phone={}", role, phone);
            return resp;
        }

        // Legacy fallback: there is an existing user (created via the old /register endpoint)
        // who simply hadn't been phone-verified yet. Keep the old behaviour.
        otpRepository.delete(record);
        log.info("OTP verified for pre-existing {} (phone={}); no new row created", role, phone);
        return role == UserRole.STUDENT ? verifyExistingStudent(phone) : verifyExistingTeacher(phone);
    }

    // =========================================================================
    // Resend — preserved for users already mid-flow (resend OTP without
    // re-uploading the ID card). Carries the same pending payload forward.
    // =========================================================================
    @Transactional
    public void sendOtp(String rawPhone, UserRole role, boolean isResend) {
        String phone = PhoneUtils.normalizeBd(rawPhone);
        PhoneVerificationOtp existing = otpRepository.findByPhoneAndUserRole(phone, role).orElse(null);
        if (existing == null) {
            // Nothing to resend — caller never went through /init
            throw new IllegalStateException(
                    "No pending registration found for this phone. Please submit the registration form first.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (isResend && existing.getLastSentAt().plusSeconds(resendCooldownSeconds).isAfter(now)) {
            throw new IllegalArgumentException("Please wait before requesting another OTP");
        }
        internalSendOtp(phone, role,
                existing.getPendingRegistrationJson(),
                existing.getPendingIdCardUrl(),
                true);
    }

    // =========================================================================
    // Internal OTP send — generates code, stores hash + reuses pending payload.
    // =========================================================================
    private void internalSendOtp(String phone, UserRole role,
                                  String pendingJson, String pendingIdCardUrl,
                                  boolean isResend) {
        LocalDateTime now = LocalDateTime.now();
        PhoneVerificationOtp existing = otpRepository.findByPhoneAndUserRole(phone, role).orElse(null);

        // Generate fresh OTP regardless of resend vs first send
        String otp = "%06d".formatted(secureRandom.nextInt(1_000_000));

        PhoneVerificationOtp record = existing == null
                ? PhoneVerificationOtp.builder()
                        .phone(phone).userRole(role).createdAt(now).build()
                : existing;
        record.setOtpHash(passwordEncoder.encode(otp));
        record.setExpiresAt(now.plusMinutes(expiryMinutes));
        record.setLastSentAt(now);
        record.setFailedAttempts(0);
        // Carry pending data on first send; keep whatever was there on resend.
        if (!isResend || record.getPendingRegistrationJson() == null) {
            record.setPendingRegistrationJson(pendingJson);
        }
        if (!isResend || record.getPendingIdCardUrl() == null) {
            record.setPendingIdCardUrl(pendingIdCardUrl);
        }
        otpRepository.save(record);

        log.info("=========================================================");
        log.info("OTP for phone={} role={} -> [DEV ONLY] OTP CODE: {} (expires in {} min)",
                phone, role, otp, expiryMinutes);
        log.info("=========================================================");

        boolean sent = smsService.sendOtpSms(phone, otp);
        if (!sent) {
            log.error("Failed to send OTP SMS to {} - check BulkSMSBD Response logs above (balance/senderId/IP)",
                    phone);
            if (!allowSoftFail) {
                throw new IllegalStateException(
                        "Failed to send OTP SMS. Please check SMS gateway or try again. " +
                                "Check server logs for BulkSMSBD Response.");
            }
            log.warn("app.sms.allow-soft-fail=true - OTP is saved in DB and printed above. " +
                    "User can still verify. Set SMS_ALLOW_SOFT_FAIL=false to enforce strict SMS delivery.");
        }
        log.info("OTP generated and SMS sent successfully to {} (expires in {} min)", phone, expiryMinutes);
    }

    // =========================================================================
    // Pending → Student / Teacher construction
    // =========================================================================
    private AuthResponse createStudentFromPending(String json, String idCardUrl) {
        Map<String, Object> p = readPayload(json);
        Student s = Student.builder()
                .name((String) p.get("name"))
                .phone((String) p.get("phone"))
                .password((String) p.get("passwordHash"))
                .studentId((String) p.get("studentId"))
                .department((String) p.get("department"))
                .varsityBatch((String) p.get("varsityBatch"))
                .idCardImageUrl(idCardUrl)
                .isVerified(true)        // phone verification auto-grants full verification (legacy behaviour)
                .isPhoneVerified(true)
                .isActive(true)
                .build();
        s = studentRepository.save(s);
        return authResponse(s.getId(), s.getName(), s.getPhone(), true, "STUDENT");
    }

    private AuthResponse createTeacherFromPending(String json, String idCardUrl) {
        Map<String, Object> p = readPayload(json);
        Teacher t = Teacher.builder()
                .name((String) p.get("name"))
                .phone((String) p.get("phone"))
                .password((String) p.get("passwordHash"))
                .teacherId((String) p.get("teacherId"))
                .designation((String) p.get("designation"))
                .department((String) p.get("department"))
                .idCardImageUrl(idCardUrl)
                .isVerified(true)        // phone verification auto-grants full verification (legacy behaviour)
                .isPhoneVerified(true)
                .isActive(true)
                .build();
        t = teacherRepository.save(t);
        return authResponse(t.getId(), t.getName(), t.getPhone(), true, "TEACHER");
    }

    // Legacy: Student/Teacher already exists (created via old /register endpoint)
    private AuthResponse verifyExistingStudent(String phone) {
        Student user = studentRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with this phone number"));
        user.setIsPhoneVerified(true);
        user.setIsVerified(true);
        studentRepository.save(user);
        return authResponse(user.getId(), user.getName(), user.getPhone(),
                user.getIsVerified(), "STUDENT");
    }

    private AuthResponse verifyExistingTeacher(String phone) {
        Teacher user = teacherRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with this phone number"));
        user.setIsPhoneVerified(true);
        user.setIsVerified(true);
        teacherRepository.save(user);
        return authResponse(user.getId(), user.getName(), user.getPhone(),
                user.getIsVerified(), "TEACHER");
    }

    private AuthResponse authResponse(Long id, String name, String phone, Boolean verified, String role) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(phone, role))
                .tokenType("Bearer")
                .role(role).id(id).name(name).phone(phone).isVerified(verified)
                .isPhoneVerified(true).build();
    }

    // =========================================================================
    // Cleanup
    // =========================================================================
    /**
     * Sweep expired OTP rows + their orphaned Cloudinary uploads. Runs every
     * 10 minutes by default (override with app.phone-verification.cleanup-interval-ms).
     */
    @Scheduled(fixedDelayString = "${app.phone-verification.cleanup-interval-ms:600000}",
               initialDelayString = "${app.phone-verification.cleanup-initial-delay-ms:60000}")
    @Transactional
    public void cleanupExpired() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(CLEANUP_GRACE_HOURS);
        List<PhoneVerificationOtp> expired = otpRepository.findAllByExpiresAtBefore(cutoff);
        if (expired.isEmpty()) return;
        int deleted = otpRepository.deleteAllExpiredSince(cutoff);
        log.info("Cleaned up {} expired OTP rows (cutoff={})", deleted, cutoff);
        for (PhoneVerificationOtp o : expired) {
            if (o.getPendingIdCardUrl() != null && !o.getPendingIdCardUrl().isBlank()) {
                safeDeleteCloudinary(o.getPendingIdCardUrl());
            }
        }
    }

    private void cleanupSingleRecord(PhoneVerificationOtp record) {
        if (record.getPendingIdCardUrl() != null && !record.getPendingIdCardUrl().isBlank()) {
            safeDeleteCloudinary(record.getPendingIdCardUrl());
        }
        otpRepository.delete(record);
    }

    private void safeDeleteCloudinary(String url) {
        try {
            cloudinaryService.deleteImage(url);
        } catch (Exception e) {
            log.warn("Cloudinary cleanup failed for {}: {}", url, e.getMessage());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    @SuppressWarnings("unchecked")
    private Map<String, Object> readPayload(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize pending registration: " + e.getMessage(), e);
        }
    }
}
