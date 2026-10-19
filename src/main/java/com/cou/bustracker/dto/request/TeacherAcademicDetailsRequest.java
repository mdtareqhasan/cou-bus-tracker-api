package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Profile details a teacher fills in exactly once, after Google Sign-In
 * (which creates the row without them).
 *
 * <p>The department is now referenced by row id (fetched from the public
 * academic reference endpoints). It may point at either a DEPARTMENT or an
 * OFFICE unit, since a teacher works in a teaching department or an
 * administrative office. Unlike registration, {@code designation} is
 * mandatory here because the app policy requires employee ID, department
 * and designation before a teacher can use the system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAcademicDetailsRequest {

    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotNull(message = "Employee ID is required")
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$",
            message = "Employee ID must be 2-50 characters")
    private String employeeId;

    @NotNull(message = "Designation is required")
    @Size(max = 100, message = "Designation must be at most 100 characters")
    private String designation;
}
