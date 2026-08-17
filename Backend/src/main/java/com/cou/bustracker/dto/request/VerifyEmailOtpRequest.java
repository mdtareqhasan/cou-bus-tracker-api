package com.cou.bustracker.dto.request;

import com.cou.bustracker.entity.EmailVerificationOtp.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailOtpRequest(@Email(message = "Please provide a valid email") String email,
                                    @NotNull(message = "Role is required") UserRole role,
                                    @NotBlank(message = "OTP is required") @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits") String otp) { }
