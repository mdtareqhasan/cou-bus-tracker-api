package com.cou.bustracker.dto.request;

import com.cou.bustracker.entity.EmailVerificationOtp.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailOtpRequest(@Email(message = "সঠিক ইমেইল দিন") String email,
                                    @NotNull(message = "রোল নির্বাচন করুন") UserRole role,
                                    @NotBlank(message = "OTP দিন") @Pattern(regexp = "\\d{6}", message = "OTP অবশ্যই ৬ সংখ্যার হতে হবে") String otp) { }
