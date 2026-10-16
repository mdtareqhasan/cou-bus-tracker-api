package com.cou.bustracker.dto.superadmin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminLoginRequest {

    @NotBlank(message = "ইমেইল দিন")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "পাসওয়ার্ড দিন")
    private String password;
}