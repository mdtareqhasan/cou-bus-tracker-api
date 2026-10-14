package com.cou.bustracker.dto.request;

import com.cou.bustracker.entity.PhoneVerificationOtp.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OTP-first registration payload. Sent as multipart/form-data along with the
 * "idCard" file. The student/teacher row is NOT created until the matching
 * OTP is verified — see {@code PhoneVerificationService.verifyOtp}.
 *
 * Fields are role-discriminated: {@code studentId} is required when
 * {@code role=STUDENT}, {@code teacherId} when {@code role=TEACHER}.
 * Validation is enforced again at service time so we don't waste an OTP on
 * bad input.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneVerificationInitRequest {

    @NotNull(message = "Role is required")
    private UserRole role;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Please provide a valid phone number")
    private String phone;

    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
    private String password;

    /** Google ID token for Google Sign-In registrations. Password is required when this is absent. */
    private String googleIdToken;

    // ---- Student-only fields (ignored when role=TEACHER) ----
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$",
             message = "Student ID must be 2-50 alphanumeric characters")
    private String studentId;

    @Pattern(regexp = "^([0-9]{1,2}|[0-9]{4}(-[0-9]{4})?)$",
             message = "Batch must be a number such as 16, 2020, or 2020-2024")
    private String varsityBatch;

    // ---- Teacher-only fields (ignored when role=STUDENT) ----
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$",
             message = "Teacher ID must be 2-50 alphanumeric characters")
    private String teacherId;

    @Size(max = 100, message = "Designation must be at most 100 characters")
    private String designation;

    // ---- Shared ----
    @NotBlank(message = "Department is required")
    @Size(min = 2, max = 100, message = "Department must be between 2 and 100 characters")
    private String department;
}
