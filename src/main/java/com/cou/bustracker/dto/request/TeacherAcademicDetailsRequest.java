package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Profile details a teacher fills in exactly once, after Google Sign-In
 * (which creates the row without them).
 *
 * <p>Validation mirrors {@link EmailVerificationInitRequest}; unlike
 * registration, {@code designation} is mandatory here because the app policy
 * requires employee ID, department and designation before a teacher can use
 * the system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAcademicDetailsRequest {

    @NotBlank(message = "Department is required")
    @Size(min = 2, max = 100, message = "Department must be 2 to 100 characters")
    private String department;

    @NotBlank(message = "Employee ID is required")
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$",
            message = "Employee ID must be 2-50 characters")
    private String employeeId;

    @NotBlank(message = "Designation is required")
    @Size(max = 100, message = "Designation must be at most 100 characters")
    private String designation;
}
