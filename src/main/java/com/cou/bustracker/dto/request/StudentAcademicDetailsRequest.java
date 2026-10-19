package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Academic details a student fills in exactly once, after Google Sign-In
 * (which creates the row without them).
 *
 * <p>Departments and sessions are now referenced by row id (fetched from the
 * public academic reference endpoints) rather than a free-text string.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAcademicDetailsRequest {

    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotNull(message = "Roll number is required")
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$",
            message = "Roll number must be 2-50 characters")
    private String rollNumber;

    @NotNull(message = "Session is required")
    private Long sessionId;
}
