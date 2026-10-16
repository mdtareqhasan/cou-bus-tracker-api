package com.cou.bustracker.dto.request;

import com.cou.bustracker.entity.EmailVerificationOtp.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record EmailVerificationRequest(@Email(message = "সঠিক ইমেইল দিন") String email,
                                       @NotNull(message = "রোল নির্বাচন করুন") UserRole role) { }
