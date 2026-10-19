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
public class TeacherResponse {

    private Long id;
    private String name;
    private String teacherId;
    private String designation;

    /**
     * The department's resolved Bengali name, for display. Kept so the admin
     * panel and Flutter profile screen render with no change; the FK id below
     * is the source of truth.
     */
    private String department;

    /**
     * FK into academic_units — may point at a DEPARTMENT or an OFFICE row,
     * since a teacher works in a teaching department or an administrative
     * office.
     */
    private Long departmentId;

    private String email;
    private String idCardImageUrl;
    /**
     * Derived, not stored: verified once department, employee ID and
     * designation are filled in — the teacher equivalent of the student
     * department/roll/session rule.
     */
    private Boolean isVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
