package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "গুগল আইডি টোকেন দিন")
    private String idToken;

    @NotNull(message = "রোল নির্বাচন করুন")
    private UserRole role;

    public enum UserRole { STUDENT, TEACHER }
}
