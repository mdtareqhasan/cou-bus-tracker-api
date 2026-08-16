package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.StudentRegisterRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.StudentResponse;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.StudentRepository;
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
public class StudentService {

    private final StudentRepository studentRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenService googleTokenService;
    private final FileStorageService fileStorageService;

    private static final String EDU_MAIL_DOMAIN = "cou.ac.bd";

    @Transactional
    public AuthResponse register(StudentRegisterRequest request, MultipartFile idCard) throws java.io.IOException {
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (studentRepository.existsByStudentId(request.getStudentId())) {
            throw new RuntimeException("Student ID already registered");
        }

        GoogleTokenService.GoogleIdentity google = resolveGoogleRegistration(
                request.getEmail(), request.getPassword(), request.getGoogleIdToken());
        boolean isEduMail = google != null || request.getEmail().endsWith("@" + EDU_MAIL_DOMAIN);

        Student student = Student.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(google == null ? passwordEncoder.encode(request.getPassword()) : null)
                .googleSubject(google == null ? null : google.subject())
                .studentId(request.getStudentId())
                .department(request.getDepartment())
                .varsityBatch(request.getVarsityBatch())
                .isEduMail(isEduMail)
                .isVerified(false)
                .isActive(true)
                .build();

        // Validate & store ID card image (enforced by FileStorageService)
        String imageUrl = fileStorageService.storeIdCard(idCard, "student-id-cards");
        student.setIdCardImageUrl(imageUrl);

        studentRepository.save(student);

        String token = jwtService.generateToken(student.getEmail(), "STUDENT");

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role("STUDENT")
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .isVerified(student.getIsVerified())
                .isEduMail(student.getIsEduMail())
                .build();
    }

    public AuthResponse login(String email, String password) {
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        if (student.getPassword() == null || !passwordEncoder.matches(password, student.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!student.getIsActive()) {
            throw new BadCredentialsException("Account is deactivated. Please contact admin.");
        }

        String token = jwtService.generateToken(student.getEmail(), "STUDENT");

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role("STUDENT")
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .isVerified(student.getIsVerified())
                .isEduMail(student.getIsEduMail())
                .build();
    }

    public AuthResponse loginWithGoogle(String idToken) {
        GoogleTokenService.GoogleIdentity identity = googleTokenService.verify(idToken);
        Student student = studentRepository.findByEmail(identity.email())
                .orElseThrow(() -> new BadCredentialsException(
                        "No student registration found. Please register first and upload your ID card."));
        if (!identity.subject().equals(student.getGoogleSubject())) {
            throw new BadCredentialsException(
                    "This Google account is not linked to any student profile. Please register first.");
        }
        if (!student.getIsActive()) {
            throw new BadCredentialsException("Account is deactivated. Please contact admin.");
        }

        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(student.getEmail(), "STUDENT"))
                .tokenType("Bearer")
                .role("STUDENT")
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .isVerified(student.getIsVerified())
                .isEduMail(student.getIsEduMail())
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
    public void uploadIdCard(Long studentId, String imageUrl) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        student.setIdCardImageUrl(imageUrl);
        studentRepository.save(student);
    }

    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<StudentResponse> getPendingStudents() {
        return studentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(s -> !s.getIsVerified())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void verifyStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        student.setIsVerified(true);
        studentRepository.save(student);
    }

    @Transactional
    public void deactivateStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        student.setIsActive(!student.getIsActive());
        studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        // Delete ID card image from filesystem if exists
        if (student.getIdCardImageUrl() != null) {
            deleteIdCardFile(student.getIdCardImageUrl());
        }
        studentRepository.delete(student);
    }

    private void deleteIdCardFile(String imageUrl) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(imageUrl).toAbsolutePath().normalize();
            java.nio.file.Files.deleteIfExists(path);
        } catch (Exception ignored) {
            // File may already be deleted or not found; log but don't fail the delete operation
        }
    }

    public Student getStudentByEmail(String email) {
        return studentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private StudentResponse mapToResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .studentId(student.getStudentId())
                .department(student.getDepartment())
                .varsityBatch(student.getVarsityBatch())
                .idCardImageUrl(student.getIdCardImageUrl())
                .isEduMail(student.getIsEduMail())
                .isVerified(student.getIsVerified())
                .isActive(student.getIsActive())
                .createdAt(student.getCreatedAt())
                .build();
    }
}
