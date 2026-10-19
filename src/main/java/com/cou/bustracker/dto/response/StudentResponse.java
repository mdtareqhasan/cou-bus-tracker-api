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

    /**
     * The department's resolved Bengali name, for display. Kept so the admin
     * panel and Flutter profile screen render with no change; the FK id below
     * is the source of truth.
     */
    private String department;

    /** Resolved Bengali name of the session's range (e.g. '২০২০-২০২১'). */
    private String session;

    /** FK into academic_units — a DEPARTMENT row. */
    private Long departmentId;

    /** FK into academic_sessions. */
    private Long sessionId;

    private String idCardImageUrl;
    /**
     * Derived, not stored: an account counts as verified once department, roll
     * number and session are filled in (the one step registration completes).
     */
    private Boolean isVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
