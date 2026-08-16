package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    private String email;

    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
    private String password;

    /** Google ID token for Google Sign-In registrations. Password is required when this is absent. */
    private String googleIdToken;

    @NotBlank(message = "Student ID is required")
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,50}$", message = "Student ID must be 2-50 alphanumeric characters")
    private String studentId;

    @NotBlank(message = "Department is required")
    @Size(min = 2, max = 100, message = "Department must be between 2 and 100 characters")
    private String department;

    @NotBlank(message = "Varsity batch is required")
    @Pattern(
            regexp = "^([0-9]{1,2}|[0-9]{4}(-[0-9]{4})?)$",
            message = "Batch must be a number such as 16, 2020, or 2020-2024")
    private String varsityBatch;
}
