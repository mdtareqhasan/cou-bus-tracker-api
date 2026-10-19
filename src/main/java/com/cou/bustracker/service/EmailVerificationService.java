package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.EmailVerificationInitRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.entity.AcademicSession;
import com.cou.bustracker.entity.AcademicUnit;
import com.cou.bustracker.entity.EmailVerificationOtp.UserRole;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.AcademicSessionRepository;
import com.cou.bustracker.repository.AcademicUnitRepository;
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
 *
 * <p>Department and session are submitted as row ids (fetched from the public
 * academic reference endpoints) and resolved to entities here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicUnitRepository academicUnitRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Simple in-memory staging for the placeholder flow.
    private final Map<String, EmailVerificationInitRequest> pendingRegistrations = new ConcurrentHashMap<>();

    public void initRegistration(EmailVerificationInitRequest request) {
        validateRequest(request);
        String key = key(request.getEmail(), request.getRole());
        pendingRegistrations.put(key, request);
        log.info("[DUMMY EMAIL VERIFICATION] initRegistration role={} email={} name={} departmentId={}",
                request.getRole(), request.getEmail(), request.getName(), request.getDepartmentId());
        // TODO: persist OTP row and send email.
    }

    public void sendOtp(String rawEmail, UserRole role, boolean isResend) {
        log.info("[DUMMY EMAIL VERIFICATION] sendOtp role={} email={} isResend={}", role, rawEmail, isResend);
        // TODO: persist OTP and send it through the real email provider.
    }

    public AuthResponse verifyOtp(String rawEmail, UserRole role, String otp) {
        if (otp == null || !otp.matches("\\d{6}")) {
            throw new ApiException(ErrorCode.OTP_INVALID_LENGTH);
        }

        String key = key(rawEmail, role);
        EmailVerificationInitRequest request = pendingRegistrations.remove(key);
        if (request == null) {
            throw new ApiException(ErrorCode.REGISTRATION_NOT_FOUND);
        }

        // Dummy verification: any 6-digit OTP is accepted.
        log.info("[DUMMY EMAIL VERIFICATION] verifyOtp succeeded for role={} email={}", role, rawEmail);

        return role == UserRole.STUDENT
                ? createStudent(request)
                : createEmployee(request);
    }

    private void validateRequest(EmailVerificationInitRequest request) {
        if (request.getRole() == null) {
            throw new ApiException(ErrorCode.ROLE_REQUIRED);
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ApiException(ErrorCode.EMAIL_REQUIRED);
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        boolean isAllowedEmailDomain = normalizedEmail.endsWith("@stud.cou.ac.bd")
                || normalizedEmail.endsWith("@cou.ac.bd");
        if (!isAllowedEmailDomain) {
            throw new ApiException(ErrorCode.EMAIL_DOMAIN_NOT_ALLOWED);
        }
        if (request.getDepartmentId() == null) {
            throw new ApiException(ErrorCode.DEPARTMENT_REQUIRED);
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ApiException(ErrorCode.PASSWORD_REQUIRED);
        }

        // The referenced unit must exist and be active.
        resolveActiveUnit(request.getDepartmentId());

        if (request.getRole() == UserRole.STUDENT) {
            if (request.getRollNumber() == null || request.getRollNumber().isBlank()) {
                throw new ApiException(ErrorCode.ROLL_NUMBER_REQUIRED);
            }
            if (request.getSessionId() == null) {
                throw new ApiException(ErrorCode.SESSION_REQUIRED);
            }
            resolveActiveSession(request.getSessionId());
            if (studentRepository.existsByEmail(normalizedEmail)) {
                throw new ApiException(ErrorCode.STUDENT_EMAIL_EXISTS);
            }
            if (studentRepository.existsByRollNumber(request.getRollNumber())) {
                throw new ApiException(ErrorCode.ROLL_ALREADY_REGISTERED);
            }
        } else {
            if (request.getEmployeeId() == null || request.getEmployeeId().isBlank()) {
                throw new ApiException(ErrorCode.EMPLOYEE_ID_REQUIRED);
            }
            if (teacherRepository.existsByEmail(normalizedEmail)) {
                throw new ApiException(ErrorCode.EMPLOYEE_EMAIL_EXISTS);
            }
            if (teacherRepository.existsByTeacherId(request.getEmployeeId())) {
                throw new ApiException(ErrorCode.TEACHER_ID_ALREADY_REGISTERED);
            }
        }
    }

    private AcademicUnit resolveActiveUnit(Long departmentId) {
        AcademicUnit unit = academicUnitRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        if (!Boolean.TRUE.equals(unit.getIsActive())) {
            throw new ApiException(ErrorCode.DEPARTMENT_REQUIRED);
        }
        return unit;
    }

    private AcademicSession resolveActiveSession(Long sessionId) {
        AcademicSession session = academicSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!Boolean.TRUE.equals(session.getIsActive())) {
            throw new ApiException(ErrorCode.SESSION_REQUIRED);
        }
        return session;
    }

    private AuthResponse createStudent(EmailVerificationInitRequest request) {
        Student student = Student.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .rollNumber(request.getRollNumber())
                .department(resolveActiveUnit(request.getDepartmentId()))
                .session(resolveActiveSession(request.getSessionId()))
                .isVerified(true)
                .isEmailVerified(true)
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
                .build();
    }

    private AuthResponse createEmployee(EmailVerificationInitRequest request) {
        Teacher teacher = Teacher.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .teacherId(request.getEmployeeId())
                .designation(request.getDesignation())
                .department(resolveActiveUnit(request.getDepartmentId()))
                .isVerified(true)
                .isEmailVerified(true)
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
                .build();
    }

    private String key(String email, UserRole role) {
        return role.name() + ":" + email.toLowerCase();
    }
}
