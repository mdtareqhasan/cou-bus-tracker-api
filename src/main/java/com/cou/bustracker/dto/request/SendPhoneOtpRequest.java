package com.cou.bustracker.dto.request;

import com.cou.bustracker.entity.PhoneVerificationOtp.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SendPhoneOtpRequest(
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Please provide a valid phone number")
    String phone,
    @NotNull(message = "Role is required")
    UserRole role
) {}
