package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.TeacherRegisterRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.TeacherResponse;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.TeacherRepository;
import com.cou.bustracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenService googleTokenService;
    private final FileStorageService fileStorageService;
    private final EmailVerificationService emailVerificationService;

    private static final String EDU_MAIL_DOMAIN = "cou.ac.bd";

    @Transactional
    public AuthResponse register(TeacherRegisterRequest request, MultipartFile idCard) throws java.io.IOException {
        if (teacherRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (teacherRepository.existsByTeacherId(request.getTeacherId())) {
            throw new RuntimeException("Teacher ID already registered");
        }

        GoogleTokenService.GoogleIdentity google = resolveGoogleRegistration(
                request.getEmail(), request.getPassword(), request.getGoogleIdToken());
        boolean isEduMail = google != null || request.getEmail().endsWith("@" + EDU_MAIL_DOMAIN);

        Teacher teacher = Teacher.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(google == null ? passwordEncoder.encode(request.getPassword()) : null)
                .googleSubject(google == null ? null : google.subject())
                .teacherId(request.getTeacherId())
                .designation(request.getDesignation())
                .department(request.getDepartment())
                .phone(request.getPhone())
                .isEduMail(isEduMail)
                .isVerified(false)
                .isEmailVerified(google != null)
                .isActive(true)
                .build();

        // Validate & store ID card image (enforced by FileStorageService)
        String imageUrl = fileStorageService.storeIdCard(idCard, "teacher-id-cards");
        teacher.setIdCardImageUrl(imageUrl);

        teacherRepository.save(teacher);
        if (google == null) {
            emailVerificationService.sendOtp(teacher.getEmail(),
                    com.cou.bustracker.entity.EmailVerificationOtp.UserRole.TEACHER, false);
        }

        return AuthResponse.builder()
                .accessToken(google == null ? null : jwtService.generateToken(teacher.getEmail(), "TEACHER"))
                .tokenType(google == null ? null : "Bearer")
                .role("TEACHER")
                .id(teacher.getId())
                .name(teacher.getName())
                .email(teacher.getEmail())
                .isVerified(teacher.getIsVerified())
                .isEmailVerified(teacher.getIsEmailVerified())
                .isEduMail(teacher.getIsEduMail())
                .build();
    }

    public AuthResponse login(String email, String password) {
        Teacher teacher = teacherRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        if (teacher.getPassword() == null || !passwordEncoder.matches(password, teacher.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!teacher.getIsActive()) {
            throw new BadCredentialsException("Account is deactivated. Please contact admin.");
        }
        if (!teacher.getIsEmailVerified()) {
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
                .isVerified(teacher.getIsVerified())
                .isEmailVerified(teacher.getIsEmailVerified())
                .isEduMail(teacher.getIsEduMail())
                .build();
    }

    public AuthResponse loginWithGoogle(String idToken) {
        GoogleTokenService.GoogleIdentity identity = googleTokenService.verify(idToken);
        Teacher teacher = teacherRepository.findByEmail(identity.email())
                .orElseThrow(() -> new BadCredentialsException(
                        "No teacher registration found. Please register first and upload your ID card."));
        if (!identity.subject().equals(teacher.getGoogleSubject())) {
            throw new BadCredentialsException(
                    "This Google account is not linked to any teacher profile. Please register first.");
        }
        if (!teacher.getIsActive()) {
            throw new BadCredentialsException("Account is deactivated. Please contact admin.");
        }
        if (!teacher.getIsEmailVerified()) {
            throw new BadCredentialsException("Please verify your email before logging in");
        }

        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(teacher.getEmail(), "TEACHER"))
                .tokenType("Bearer")
                .role("TEACHER")
                .id(teacher.getId())
                .name(teacher.getName())
                .email(teacher.getEmail())
                .isVerified(teacher.getIsVerified())
                .isEmailVerified(teacher.getIsEmailVerified())
                .isEduMail(teacher.getIsEduMail())
                .build();
    }

    private GoogleTokenService.GoogleIdentity resolveGoogleRegistration(
            String email, String password, String googleIdToken) {
        if (googleIdToken == null || googleIdToken.isBlank()) {
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException(
                        "Password is required when not registering with Google");
            }
            return null;
        }
        GoogleTokenService.GoogleIdentity identity = googleTokenService.verify(googleIdToken);
        if (!identity.email().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "Registration email must match the Google account email");
        }
        return identity;
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
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Teacher getTeacherByEmail(String email) {
        return teacherRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
    }

    public TeacherResponse getProfile(String email) {
        return mapToResponse(getTeacherByEmail(email));
    }

    public List<TeacherResponse> getPendingTeachers() {
        return teacherRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(t -> !t.getIsVerified())
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
        // Delete ID card image from filesystem if exists
        if (teacher.getIdCardImageUrl() != null) {
            deleteIdCardFile(teacher.getIdCardImageUrl());
        }
        teacherRepository.delete(teacher);
    }

    private void deleteIdCardFile(String imageUrl) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(imageUrl).toAbsolutePath().normalize();
            java.nio.file.Files.deleteIfExists(path);
        } catch (Exception ignored) {
            // File may already be deleted or not found; log but don't fail the delete operation
        }
    }

    private TeacherResponse mapToResponse(Teacher teacher) {
        return TeacherResponse.builder()
                .id(teacher.getId())
                .name(teacher.getName())
                .email(teacher.getEmail())
                .teacherId(teacher.getTeacherId())
                .designation(teacher.getDesignation())
                .department(teacher.getDepartment())
                .phone(teacher.getPhone())
                .idCardImageUrl(teacher.getIdCardImageUrl())
                .isEduMail(teacher.getIsEduMail())
                .isVerified(teacher.getIsVerified())
                .isActive(teacher.getIsActive())
                .createdAt(teacher.getCreatedAt())
                .build();
    }
}
