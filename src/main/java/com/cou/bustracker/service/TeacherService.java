package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.TeacherAcademicDetailsRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.TeacherResponse;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.exception.ResourceNotFoundException;
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
 * Teacher read/write operations used after the user has been verified by
 * {@link PhoneVerificationService}. Registration itself lives in the OTP
 * service — see {@code POST /api/auth/phone-verification/init}. Google
 * sign-in may also create a pending profile — see {@link #loginWithGoogle}.
 */
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenService googleTokenService;
    private final FileStorageService fileStorageService;

    public AuthResponse loginWithPhone(String phone, String password) {
        String normalized = normalizePhone(phone);
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
                .isVerified(isComplete(teacher))
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
                .isVerified(isComplete(teacher))
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
     *
     * <p>An email already registered as a student is rejected as well: one
     * account, one role. The student flow mirrors this check.
     */
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleTokenService.GoogleIdentity identity = googleTokenService.verify(idToken);
        String email = identity.email();

        if (!GoogleTokenService.isFacultyEmail(email)) {
            throw new ApiException(ErrorCode.FACULTY_EMAIL_REQUIRED);
        }

        if (studentRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_REGISTERED_AS_STUDENT);
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
                throw new ApiException(ErrorCode.ACCOUNT_DEACTIVATED);
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
                .isVerified(isComplete(teacher))
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

    /**
     * Fills in the details a Google Sign-In account is created without:
     * department, employee ID and designation. The app policy requires all
     * three before a teacher may use the system.
     *
     * <p>Every value is write-once — once a field holds a value it can only be
     * re-sent unchanged, so an employee ID registered during sign-up can never
     * be swapped for another one. An employee ID that already belongs to a
     * different account is rejected.
     *
     * @param principal JWT subject (email for Google/email logins, phone otherwise)
     * @return the updated profile
     */
    @Transactional
    public TeacherResponse updateAcademicDetails(String principal, TeacherAcademicDetailsRequest request) {
        Teacher teacher = getTeacherByPrincipal(principal);

        String department = trimToNull(request.getDepartment());
        String employeeId = trimToNull(request.getEmployeeId());
        String designation = trimToNull(request.getDesignation());

        requireValue(ErrorCode.DEPARTMENT_REQUIRED, department);
        requireValue(ErrorCode.EMPLOYEE_ID_REQUIRED, employeeId);
        requireValue(ErrorCode.DESIGNATION_REQUIRED, designation);

        requireUnchanged(ErrorCode.DEPARTMENT_IMMUTABLE, teacher.getDepartment(), department);
        requireUnchanged(ErrorCode.EMPLOYEE_ID_IMMUTABLE, teacher.getTeacherId(), employeeId);
        requireUnchanged(ErrorCode.DESIGNATION_IMMUTABLE, teacher.getDesignation(), designation);

        boolean changed = false;
        if (isBlank(teacher.getDepartment())) {
            teacher.setDepartment(department);
            changed = true;
        }
        if (isBlank(teacher.getTeacherId())) {
            if (teacherRepository.existsByTeacherId(employeeId)) {
                throw new ApiException(ErrorCode.TEACHER_ID_ALREADY_REGISTERED);
            }
            teacher.setTeacherId(employeeId);
            changed = true;
        }
        if (isBlank(teacher.getDesignation())) {
            teacher.setDesignation(designation);
            changed = true;
        }

        return mapToResponse(changed ? teacherRepository.save(teacher) : teacher);
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
     * Every registered teacher, whatever their verification state. See
     * {@link StudentService#getAllStudents()} — the same rule applies here:
     * Google/e-mail sign-ups never set {@code isPhoneVerified}, so filtering on
     * it hid them from the admin panel.
     */
    public List<TeacherResponse> getAllTeachers() {
        return teacherRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Teacher getTeacherByPhone(String phone) {
        return teacherRepository.findByPhone(normalizePhone(phone))
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

    /** Accounts still awaiting the registration step, i.e. academic details missing. */
    public List<TeacherResponse> getPendingTeachers() {
        return teacherRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(teacher -> !isComplete(teacher))
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
                .email(teacher.getEmail())
                .idCardImageUrl(teacher.getIdCardImageUrl())
                .isVerified(isComplete(teacher))
                .isActive(teacher.getIsActive())
                .createdAt(teacher.getCreatedAt())
                .build();
    }

    /**
     * Verification is the single registration step: department, employee ID and
     * designation are submitted together, and once they are all there the
     * account is verified. Derived on read — the stored column is untouched.
     * Teacher mirror of {@link StudentService#isComplete(Student)}.
     */
    public static boolean isComplete(Teacher teacher) {
        return isNotBlank(teacher.getDepartment())
                && isNotBlank(teacher.getTeacherId())
                && isNotBlank(teacher.getDesignation());
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("880") && cleaned.length() == 13) {
            return cleaned.substring(2);
        }
        if (cleaned.startsWith("01") && cleaned.length() == 11) {
            return cleaned;
        }
        if (cleaned.length() == 10) {
            return "0" + cleaned;
        }
        return cleaned;
    }
}
