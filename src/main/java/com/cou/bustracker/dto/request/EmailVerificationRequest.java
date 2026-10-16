package com.cou.bustracker.dto.request;

import com.cou.bustracker.entity.EmailVerificationOtp.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record EmailVerificationRequest(@Email(message = "Enter a valid email address") String email,
                                       @NotNull(message = "Select a role") UserRole role) { }
