package com.cou.bustracker.dto.request;

import com.cou.bustracker.entity.EmailVerificationOtp.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record EmailVerificationRequest(@Email(message = "Please provide a valid email") String email,
                                       @NotNull(message = "Role is required") UserRole role) { }
