package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "ইমেইল দিন")
    @Email(message = "সঠিক ইমেইল দিন")
    private String email;

    @NotBlank(message = "পাসওয়ার্ড দিন")
    private String password;
}
