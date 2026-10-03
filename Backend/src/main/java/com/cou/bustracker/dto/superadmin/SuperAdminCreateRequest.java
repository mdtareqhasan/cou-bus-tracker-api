package com.cou.bustracker.dto.superadmin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminCreateRequest {

    @NotBlank(message = "ইমেইল দিন")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "পাসওয়ার্ড দিন")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private Boolean isActive = Boolean.TRUE;
}