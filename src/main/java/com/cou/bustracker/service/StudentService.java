package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.StudentAcademicDetailsRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.StudentResponse;
import com.cou.bustracker.entity.AcademicSession;
import com.cou.bustracker.entity.AcademicUnit;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.AcademicSessionRepository;
import com.cou.bustracker.repository.AcademicUnitRepository;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.TeacherRepository;
import com.cou.bustracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Student read/write operations used after the user has been verified by
 * {@link EmailVerificationService}. Google sign-in may also create a pending
 * profile — see {@link #loginWithGoogle}.
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicUnitRepository academicUnitRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenService googleTokenService;
    private final FileStorageService fileStorageService;

    public AuthResponse loginWithEmail(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase();
        Student student = studentRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (student.getPassword() == null || !passwordEncoder.matches(password, student.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!student.getIsActive()) {
            throw new BadCredentialsException("Account is deactivated. Please contact admin.");
        }
        if (!Boolean.TRUE.equals(student.getIsEmailVerified())) {
            throw new BadCredentialsException("Please verify your email before logging in");
        }

        String token = jwtService.generateToken(student.getEmail(), "STUDENT");

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role("STUDENT")
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .isVerified(isComplete(student))
                .build();
    }

    /**
     * Google Sign-In. Verifies the ID token and signs the student in. If no
     * student row exists for the (verified) institutional email, a pending
     * profile is created — academic details and a password are filled in later.
     *
     * <p>An email already registered as a teacher is rejected: one account,
     * one role. The teacher flow mirrors this check.
     */
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleTokenService.GoogleIdentity identity = googleTokenService.verify(idToken);
        String email = identity.email();

        if (teacherRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_REGISTERED_AS_TEACHER);
        }

        Student student = studentRepository.findByEmail(email).orElse(null);
        if (student == null) {
            student = studentRepository.save(Student.builder()
                    .name(identity.displayName())
                    .email(email)
                    .googleSubject(identity.subject())
                    .isEmailVerified(true)
                    .isVerified(false)
                    .isActive(true)
                    .build());
        } else {
            if (!Boolean.TRUE.equals(student.getIsActive())) {
                throw new ApiException(ErrorCode.ACCOUNT_DEACTIVATED);
            }
            boolean changed = false;
            if (student.getGoogleSubject() == null) {
                student.setGoogleSubject(identity.subject());
                changed = true;
            }
            // Google has already verified this address.
            if (!Boolean.TRUE.equals(student.getIsEmailVerified())) {
                student.setIsEmailVerified(true);
                changed = true;
            }
            if (changed) {
                studentRepository.save(student);
            }
        }

        String token = jwtService.generateToken(student.getEmail(), "STUDENT");

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role("STUDENT")
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .isVerified(isComplete(student))
                .build();
    }

    @Transactional
    public void uploadIdCard(Long studentId, String imageUrl) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        student.setIdCardImageUrl(imageUrl);
        studentRepository.save(student);
    }

    /**
     * Fills in the academic details a Google Sign-In account is created
     * without: department, roll number and session. The app policy requires all
     * three before a student may use the system.
     *
     * <p>Department and session are referenced by row id (fetched from the
     * public academic reference endpoints). A student must pick a
     * {@code DEPARTMENT} unit — offices are teacher-only.
     *
     * <p>Every value is write-once — once a field holds a value it can only be
     * re-sent unchanged, so a roll number registered during sign-up can never
     * be swapped for another one. A roll number that already belongs to a
     * different account is rejected.
     *
     * @param principal JWT subject (email for Google/e-mail logins)
     * @return the updated profile
     */
    @Transactional
    public StudentResponse updateAcademicDetails(String principal, StudentAcademicDetailsRequest request) {
        Student student = getStudentByPrincipal(principal);

        Long departmentId = request.getDepartmentId();
        String rollNumber = trimToNull(request.getRollNumber());
        Long sessionId = request.getSessionId();

        requireValue(ErrorCode.DEPARTMENT_REQUIRED, departmentId);
        requireValue(ErrorCode.ROLL_NUMBER_REQUIRED, rollNumber);
        requireValue(ErrorCode.SESSION_REQUIRED, sessionId);

        AcademicUnit department = academicUnitRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        // Students may only pick a teaching department, never an office.
        if (department.getUnitType() != AcademicUnit.UnitType.DEPARTMENT
                || !Boolean.TRUE.equals(department.getIsActive())) {
            throw new ApiException(ErrorCode.DEPARTMENT_REQUIRED);
        }

        AcademicSession session = academicSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!Boolean.TRUE.equals(session.getIsActive())) {
            throw new ApiException(ErrorCode.SESSION_REQUIRED);
        }

        requireUnchanged(ErrorCode.DEPARTMENT_IMMUTABLE, student.getDepartment(), department);
        requireUnchanged(ErrorCode.SESSION_IMMUTABLE, student.getSession(), session);
        requireUnchanged(ErrorCode.ROLL_NUMBER_IMMUTABLE, student.getRollNumber(), rollNumber);

        boolean changed = false;
        if (student.getDepartment() == null) {
            student.setDepartment(department);
            changed = true;
        }
        if (student.getRollNumber() == null || student.getRollNumber().isBlank()) {
            if (studentRepository.existsByRollNumber(rollNumber)) {
                throw new ApiException(ErrorCode.ROLL_ALREADY_REGISTERED);
            }
            student.setRollNumber(rollNumber);
            changed = true;
        }
        if (student.getSession() == null) {
            student.setSession(session);
            changed = true;
        }

        return mapToResponse(changed ? studentRepository.save(student) : student);
    }

    /** Write-once rule: a populated value may only be re-sent unchanged. */
    private void requireUnchanged(ErrorCode errorCode, Object current, Object incoming) {
        if (current != null && !current.equals(incoming)) {
            throw new ApiException(errorCode);
        }
    }

    private void requireValue(ErrorCode errorCode, Object value) {
        if (value == null) {
            throw new ApiException(errorCode);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Every registered student, whatever their verification state.
     *
     * <p>These listings used to drop accounts with {@code isPhoneVerified = false},
     * which silently hid every Google/e-mail sign-up from the admin panel. Verification
     * is surfaced as a badge/filter in the UI instead, so nothing is filtered out here.
     */
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Accounts still awaiting the registration step, i.e. academic details missing. */
    public List<StudentResponse> getPendingStudents() {
        return studentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(student -> !isComplete(student))
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
        if (student.getIdCardImageUrl() != null) {
            deleteIdCardFile(student.getIdCardImageUrl());
        }
        studentRepository.delete(student);
    }

    private void deleteIdCardFile(String imageUrl) {
        fileStorageService.deleteFile(imageUrl);
    }

    /** Resolves a student from the JWT subject, which is an email. */
    public Student getStudentByPrincipal(String principal) {
        return studentRepository.findByEmail(principal)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    public StudentResponse getProfile(String principal) {
        return mapToResponse(getStudentByPrincipal(principal));
    }

    private StudentResponse mapToResponse(Student student) {
        AcademicUnit department = student.getDepartment();
        AcademicSession session = student.getSession();
        return StudentResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .rollNumber(student.getRollNumber())
                .department(department == null ? null : department.getNameBn())
                .session(session == null ? null : session.getNameBn())
                .departmentId(department == null ? null : department.getId())
                .sessionId(session == null ? null : session.getId())
                .idCardImageUrl(student.getIdCardImageUrl())
                .isVerified(isComplete(student))
                .isActive(student.getIsActive())
                .createdAt(student.getCreatedAt())
                .build();
    }

    /**
     * Verification is the single registration step: department, roll number and
     * session are submitted together, and once they are all there the account
     * is verified. Derived on read — the stored {@code is_verified} column is
     * left untouched.
     */
    public static boolean isComplete(Student student) {
        return student.getDepartment() != null
                && student.getRollNumber() != null && !student.getRollNumber().isBlank()
                && student.getSession() != null;
    }
}
