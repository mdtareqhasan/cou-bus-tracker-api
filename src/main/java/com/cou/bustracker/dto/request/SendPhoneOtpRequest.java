package com.cou.bustracker.dto.request;

import com.cou.bustracker.entity.PhoneVerificationOtp.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SendPhoneOtpRequest(
    @NotBlank(message = "ফোন নম্বর দিন")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "সঠিক ফোন নম্বর দিন")
    String phone,
    @NotNull(message = "রোল নির্বাচন করুন")
    UserRole role
) {}
