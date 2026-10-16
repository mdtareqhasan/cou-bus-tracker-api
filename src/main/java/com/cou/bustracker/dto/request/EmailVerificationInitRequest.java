package com.cou.bustracker.dto.request;

import com.cou.bustracker.entity.EmailVerificationOtp.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationInitRequest {

    @NotNull(message = "Select a role")
    private UserRole role;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2 to 100 characters")
    private String name;

    @Email(message = "Enter a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @Size(min = 6, max = 128, message = "Password must be 6 to 128 characters")
    private String password;

    /** Google ID token for Google Sign-In registrations. Password is required when this is absent. */
    private String googleIdToken;

    // ---- Student-only fields ----
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$",
            message = "Roll number must be 2-50 characters")
    private String rollNumber;

    @Pattern(regexp = "^([0-9]{1,2}|[0-9]{4}(-[0-9]{2,4})?)$",
            message = "Enter a valid session (e.g. 16, 2020, 2021-22, or 2020-2024)")
    private String session;

    // ---- Employee-only fields ----
    /** Accepted as employeeId in the API, mapped to existing teachers.teacher_id. */
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$",
            message = "Employee ID must be 2-50 characters")
    private String employeeId;

    @Size(max = 100, message = "Designation must be at most 100 characters")
    private String designation;

    @NotBlank(message = "Department is required")
    @Size(min = 2, max = 100, message = "Department must be 2 to 100 characters")
    private String department;
}
