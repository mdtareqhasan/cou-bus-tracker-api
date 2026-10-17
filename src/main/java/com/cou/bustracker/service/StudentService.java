package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.StudentAcademicDetailsRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.StudentResponse;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.StudentRepository;
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
 * Student read/write operations used after the user has been verified by
 * {@link PhoneVerificationService}. Registration itself lives in the OTP
 * service — see {@code POST /api/auth/phone-verification/init}. Google
 * sign-in may also create a pending profile — see {@link #loginWithGoogle}.
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenService googleTokenService;
    private final FileStorageService fileStorageService;

    public AuthResponse loginWithPhone(String phone, String password) {
        String normalized = PhoneUtils.normalizeBd(phone);
        Student student = studentRepository.findByPhone(normalized)
                .orElseThrow(() -> new RuntimeException("Student not found with this phone number"));
        if (student.getPassword() == null || !passwordEncoder.matches(password, student.getPassword())) {
            throw new BadCredentialsException("Invalid phone number or password");
        }
        if (!student.getIsActive()) {
            throw new BadCredentialsException("Account is deactivated. Please contact admin.");
        }
        if (!student.getIsPhoneVerified()) {
            throw new BadCredentialsException("Please verify your phone number before logging in");
        }

        String token = jwtService.generateToken(student.getPhone(), "STUDENT");

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role("STUDENT")
                .id(student.getId())
                .name(student.getName())
                .phone(student.getPhone())
                .isVerified(isComplete(student))
                .isPhoneVerified(student.getIsPhoneVerified())
                .build();
    }

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
                .phone(student.getPhone())
                .isVerified(isComplete(student))
                .isPhoneVerified(student.getIsPhoneVerified())
                .build();
    }

    /**
     * Google Sign-In. Verifies the ID token and signs the student in. If no
     * student row exists for the (verified) institutional email, a pending
     * profile is created — academic details and a password are filled in later.
     */
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleTokenService.GoogleIdentity identity = googleTokenService.verify(idToken);
        String email = identity.email();

        Student student = studentRepository.findByEmail(email).orElse(null);
        if (student == null) {
            student = studentRepository.save(Student.builder()
                    .name(identity.displayName())
                    .email(email)
                    .googleSubject(identity.subject())
                    .isEmailVerified(true)
                    .isVerified(false)
                    .isPhoneVerified(false)
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
                .phone(student.getPhone())
                .isVerified(isComplete(student))
                .isPhoneVerified(student.getIsPhoneVerified())
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
     * <p>Every value is write-once — once a field holds a value it can only be
     * re-sent unchanged, so a roll number registered during sign-up can never
     * be swapped for another one. A roll number that already belongs to a
     * different account is rejected.
     *
     * @param principal JWT subject (email for Google/email logins, phone otherwise)
     * @return the updated profile
     */
    @Transactional
    public StudentResponse updateAcademicDetails(String principal, StudentAcademicDetailsRequest request) {
        Student student = getStudentByPrincipal(principal);

        String department = trimToNull(request.getDepartment());
        String rollNumber = trimToNull(request.getRollNumber());
        String session = trimToNull(request.getSession());

        requireValue(ErrorCode.DEPARTMENT_REQUIRED, department);
        requireValue(ErrorCode.ROLL_NUMBER_REQUIRED, rollNumber);
        requireValue(ErrorCode.SESSION_REQUIRED, session);

        requireUnchanged(ErrorCode.DEPARTMENT_IMMUTABLE, student.getDepartment(), department);
        requireUnchanged(ErrorCode.ROLL_NUMBER_IMMUTABLE, student.getRollNumber(), rollNumber);
        requireUnchanged(ErrorCode.SESSION_IMMUTABLE, student.getSession(), session);

        boolean changed = false;
        if (isBlank(student.getDepartment())) {
            student.setDepartment(department);
            changed = true;
        }
        if (isBlank(student.getRollNumber())) {
            if (studentRepository.existsByRollNumber(rollNumber)) {
                throw new ApiException(ErrorCode.ROLL_ALREADY_REGISTERED);
            }
            student.setRollNumber(rollNumber);
            changed = true;
        }
        if (isBlank(student.getSession())) {
            student.setSession(session);
            changed = true;
        }

        return mapToResponse(changed ? studentRepository.save(student) : student);
    }

    /** Write-once rule: a populated value may only be re-sent unchanged. */
    private void requireUnchanged(ErrorCode errorCode, String current, String incoming) {
        if (!isBlank(current) && !current.trim().equals(incoming)) {
            throw new ApiException(errorCode);
        }
    }

    private void requireValue(ErrorCode errorCode, String value) {
        if (value == null) {
            throw new ApiException(errorCode);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
     * which silently hid every Google/e-mail sign-up from the admin panel (only the
     * phone-OTP flow sets that flag). Verification is surfaced as a badge/filter in
     * the UI instead, so nothing is filtered out here.
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

    public Student getStudentByPhone(String phone) {
        return studentRepository.findByPhone(PhoneUtils.normalizeBd(phone))
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    /**
     * Resolves a student from the JWT subject, which is a phone number for
     * phone logins and an email for email/Google logins.
     */
    public Student getStudentByPrincipal(String principal) {
        return studentRepository.findByPhoneOrEmail(principal)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private StudentResponse mapToResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .rollNumber(student.getRollNumber())
                .department(student.getDepartment())
                .session(student.getSession())
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
        return !isBlank(student.getDepartment())
                && !isBlank(student.getRollNumber())
                && !isBlank(student.getSession());
    }
}
