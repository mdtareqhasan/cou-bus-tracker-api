package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Academic details a student fills in exactly once, after Google Sign-In
 * (which creates the row without them).
 *
 * <p>Validation mirrors {@link EmailVerificationInitRequest} so a profile
 * completed here is indistinguishable from one completed during registration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAcademicDetailsRequest {

    @NotBlank(message = "Department is required")
    @Size(min = 2, max = 100, message = "Department must be 2 to 100 characters")
    private String department;

    @NotBlank(message = "Roll number is required")
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$",
            message = "Roll number must be 2-50 characters")
    private String rollNumber;

    @NotBlank(message = "Session is required")
    @Pattern(regexp = "^([0-9]{1,2}|[0-9]{4}(-[0-9]{2,4})?)$",
            message = "Enter a valid session (e.g. 16, 2020, 2021-22, or 2020-2024)")
    private String session;
}
