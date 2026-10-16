package com.cou.bustracker.service;

import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.TeacherResponse;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.TeacherRepository;
import com.cou.bustracker.security.JwtService;
import com.cou.bustracker.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Teacher read/write operations used after the user has been verified by
 * {@link PhoneVerificationService}. Registration itself lives in the OTP
 * service — see {@code POST /api/auth/phone-verification/init}. Google
 * sign-in may also create a pending profile — see {@link #loginWithGoogle}.
 */
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenService googleTokenService;
    private final FileStorageService fileStorageService;

    public AuthResponse loginWithPhone(String phone, String password) {
        String normalized = PhoneUtils.normalizeBd(phone);
        Teacher teacher = teacherRepository.findByPhone(normalized)
                .orElseThrow(() -> new RuntimeException("Teacher not found with this phone number"));
        if (teacher.getPassword() == null || !passwordEncoder.matches(password, teacher.getPassword())) {
            throw new BadCredentialsException("Invalid phone number or password");
        }
        if (!teacher.getIsActive()) {
            throw new BadCredentialsException("Account is deactivated. Please contact admin.");
        }
        if (!teacher.getIsPhoneVerified()) {
            throw new BadCredentialsException("Please verify your phone number before logging in");
        }

        String token = jwtService.generateToken(teacher.getPhone(), "TEACHER");

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role("TEACHER")
                .id(teacher.getId())
                .name(teacher.getName())
                .phone(teacher.getPhone())
                .isVerified(teacher.getIsVerified())
                .isPhoneVerified(teacher.getIsPhoneVerified())
                .build();
    }

    public AuthResponse loginWithEmail(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase();
        Teacher teacher = teacherRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (teacher.getPassword() == null || !passwordEncoder.matches(password, teacher.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!teacher.getIsActive()) {
            throw new BadCredentialsException("Account is deactivated. Please contact admin.");
        }
        if (!Boolean.TRUE.equals(teacher.getIsEmailVerified())) {
            throw new BadCredentialsException("Please verify your email before logging in");
        }

        String token = jwtService.generateToken(teacher.getEmail(), "TEACHER");

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role("TEACHER")
                .id(teacher.getId())
                .name(teacher.getName())
                .email(teacher.getEmail())
                .phone(teacher.getPhone())
                .isVerified(teacher.getIsVerified())
                .isPhoneVerified(teacher.getIsPhoneVerified())
                .build();
    }

    /**
     * Google Sign-In. Verifies the ID token and signs the teacher in. If no
     * teacher row exists for the (verified) institutional email, a pending
     * profile is created — details and a password are filled in later.
     *
     * <p>Only the faculty/staff domain ({@code name@cou.ac.bd}) is accepted
     * here: student addresses ({@code name@stud.cou.ac.bd}) are rejected, as
     * those belong to the {@code STUDENT} role.
     */
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleTokenService.GoogleIdentity identity = googleTokenService.verify(idToken);
        String email = identity.email();

        if (!GoogleTokenService.isFacultyEmail(email)) {
            throw new BadCredentialsException(
                    "শিক্ষক হিসেবে প্রবেশ করতে শুধুমাত্র কর্মচারীদের ইমেইল (name@cou.ac.bd) ব্যবহার করুন। "
                            + "শিক্ষার্থী হিসেবে প্রবেশ করতে রোল হিসেবে STUDENT নির্বাচন করুন।");
        }

        Teacher teacher = teacherRepository.findByEmail(email).orElse(null);
        if (teacher == null) {
            teacher = teacherRepository.save(Teacher.builder()
                    .name(identity.displayName())
                    .email(email)
                    .googleSubject(identity.subject())
                    .isEmailVerified(true)
                    .isVerified(false)
                    .isPhoneVerified(false)
                    .isActive(true)
                    .build());
        } else {
            if (!Boolean.TRUE.equals(teacher.getIsActive())) {
                throw new BadCredentialsException(
                        "আপনার অ্যাকাউন্ট নিষ্ক্রিয়। অ্যাডমিনের সাথে যোগাযোগ করুন।");
            }
            boolean changed = false;
            if (teacher.getGoogleSubject() == null) {
                teacher.setGoogleSubject(identity.subject());
                changed = true;
            }
            // Google has already verified this address.
            if (!Boolean.TRUE.equals(teacher.getIsEmailVerified())) {
                teacher.setIsEmailVerified(true);
                changed = true;
            }
            if (changed) {
                teacherRepository.save(teacher);
            }
        }

        String token = jwtService.generateToken(teacher.getEmail(), "TEACHER");

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role("TEACHER")
                .id(teacher.getId())
                .name(teacher.getName())
                .email(teacher.getEmail())
                .phone(teacher.getPhone())
                .isVerified(teacher.getIsVerified())
                .isPhoneVerified(teacher.getIsPhoneVerified())
                .build();
    }

    @Transactional
    public void uploadIdCard(Long teacherId, String imageUrl) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        teacher.setIdCardImageUrl(imageUrl);
        teacherRepository.save(teacher);
    }

    public List<TeacherResponse> getAllTeachers() {
        return teacherRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(teacher -> Boolean.TRUE.equals(teacher.getIsPhoneVerified()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Teacher getTeacherByPhone(String phone) {
        return teacherRepository.findByPhone(PhoneUtils.normalizeBd(phone))
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
    }

    /**
     * Resolves a teacher from the JWT subject, which is a phone number for
     * phone logins and an email for email/Google logins.
     */
    public Teacher getTeacherByPrincipal(String principal) {
        return teacherRepository.findByPhoneOrEmail(principal)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
    }

    public TeacherResponse getProfile(String principal) {
        return mapToResponse(getTeacherByPrincipal(principal));
    }

    public List<TeacherResponse> getPendingTeachers() {
        return teacherRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(teacher -> Boolean.TRUE.equals(teacher.getIsPhoneVerified()))
                .filter(teacher -> !Boolean.TRUE.equals(teacher.getIsVerified()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void verifyTeacher(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        teacher.setIsVerified(true);
        teacherRepository.save(teacher);
    }

    @Transactional
    public void deactivateTeacher(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        teacher.setIsActive(!teacher.getIsActive());
        teacherRepository.save(teacher);
    }

    @Transactional
    public void deleteTeacher(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        if (teacher.getIdCardImageUrl() != null) {
            deleteIdCardFile(teacher.getIdCardImageUrl());
        }
        teacherRepository.delete(teacher);
    }

    private void deleteIdCardFile(String imageUrl) {
        fileStorageService.deleteFile(imageUrl);
    }

    private TeacherResponse mapToResponse(Teacher teacher) {
        return TeacherResponse.builder()
                .id(teacher.getId())
                .name(teacher.getName())
                .teacherId(teacher.getTeacherId())
                .designation(teacher.getDesignation())
                .department(teacher.getDepartment())
                .phone(teacher.getPhone())
                .email(teacher.getEmail())
                .idCardImageUrl(teacher.getIdCardImageUrl())
                .isVerified(teacher.getIsVerified())
                .isPhoneVerified(teacher.getIsPhoneVerified())
                .isActive(teacher.getIsActive())
                .createdAt(teacher.getCreatedAt())
                .build();
    }
}
