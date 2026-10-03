package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.EmailVerificationInitRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.entity.EmailVerificationOtp.UserRole;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.TeacherRepository;
import com.cou.bustracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Placeholder service while the real email verification flow is being built.
 * The password is validated, a dummy OTP is accepted, and then the user is created.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Simple in-memory staging for the placeholder flow.
    private final Map<String, EmailVerificationInitRequest> pendingRegistrations = new ConcurrentHashMap<>();

    public void initRegistration(EmailVerificationInitRequest request) {
        validateRequest(request);
        String key = key(request.getEmail(), request.getRole());
        pendingRegistrations.put(key, request);
        log.info("[DUMMY EMAIL VERIFICATION] initRegistration role={} email={} name={} department={}",
                request.getRole(), request.getEmail(), request.getName(), request.getDepartment());
        // TODO: persist OTP row and send email.
    }

    public void sendOtp(String rawEmail, UserRole role, boolean isResend) {
        log.info("[DUMMY EMAIL VERIFICATION] sendOtp role={} email={} isResend={}", role, rawEmail, isResend);
        // TODO: persist OTP and send it through the real email provider.
    }

    public AuthResponse verifyOtp(String rawEmail, UserRole role, String otp) {
        if (otp == null || !otp.matches("\\d{6}")) {
            throw new IllegalArgumentException("OTP must be 6 digits");
        }

        String key = key(rawEmail, role);
        EmailVerificationInitRequest request = pendingRegistrations.remove(key);
        if (request == null) {
            throw new IllegalArgumentException("No pending registration found. Please submit the registration form first.");
        }

        // Dummy verification: any 6-digit OTP is accepted.
        log.info("[DUMMY EMAIL VERIFICATION] verifyOtp succeeded for role={} email={}", role, rawEmail);

        return role == UserRole.STUDENT
                ? createStudent(request)
                : createEmployee(request);
    }

    private void validateRequest(EmailVerificationInitRequest request) {
        if (request.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getDepartment() == null || request.getDepartment().isBlank()) {
            throw new IllegalArgumentException("Department is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (request.getRole() == UserRole.STUDENT) {
            if (request.getRollNumber() == null || request.getRollNumber().isBlank()) {
                throw new IllegalArgumentException("Roll number is required");
            }
            if (request.getSession() == null || request.getSession().isBlank()) {
                throw new IllegalArgumentException("Session is required");
            }
            if (studentRepository.existsByEmail(request.getEmail().toLowerCase())) {
                throw new IllegalStateException("A student with this email already exists");
            }
            if (studentRepository.existsByRollNumber(request.getRollNumber())) {
                throw new IllegalStateException("This roll number is already registered");
            }
        } else {
            if (request.getEmployeeId() == null || request.getEmployeeId().isBlank()) {
                throw new IllegalArgumentException("Employee ID is required");
            }
            if (teacherRepository.existsByEmail(request.getEmail().toLowerCase())) {
                throw new IllegalStateException("An employee with this email already exists");
            }
            if (teacherRepository.existsByTeacherId(request.getEmployeeId())) {
                throw new IllegalStateException("This employee ID is already registered");
            }
        }
    }

    private AuthResponse createStudent(EmailVerificationInitRequest request) {
        Student student = Student.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .rollNumber(request.getRollNumber())
                .department(request.getDepartment())
                .session(request.getSession())
                .isVerified(true)
                .isEmailVerified(true)
                .isPhoneVerified(false)
                .isActive(true)
                .build();

        student = studentRepository.save(student);
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(student.getEmail(), "STUDENT"))
                .tokenType("Bearer")
                .role("STUDENT")
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .isVerified(true)
                .isPhoneVerified(false)
                .build();
    }

    private AuthResponse createEmployee(EmailVerificationInitRequest request) {
        Teacher teacher = Teacher.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .teacherId(request.getEmployeeId())
                .designation(request.getDesignation())
                .department(request.getDepartment())
                .isVerified(true)
                .isEmailVerified(true)
                .isPhoneVerified(false)
                .isActive(true)
                .build();

        teacher = teacherRepository.save(teacher);
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(teacher.getEmail(), "TEACHER"))
                .tokenType("Bearer")
                .role("TEACHER")
                .id(teacher.getId())
                .name(teacher.getName())
                .email(teacher.getEmail())
                .isVerified(true)
                .isPhoneVerified(false)
                .build();
    }

    private String key(String email, UserRole role) {
        return role.name() + ":" + email.toLowerCase();
    }
}
