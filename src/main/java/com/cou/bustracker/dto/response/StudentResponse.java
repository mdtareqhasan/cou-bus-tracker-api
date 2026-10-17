package com.cou.bustracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {

    private Long id;
    private String name;
    private String email;
    private String rollNumber;
    private String department;
    private String session;
    private String idCardImageUrl;
    /**
     * Derived, not stored: an account counts as verified once department, roll
     * number and session are filled in (the one step registration completes).
     * Phone is no longer part of the admin-facing payload.
     */
    private Boolean isVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
