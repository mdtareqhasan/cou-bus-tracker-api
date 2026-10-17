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
    private String department;
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
