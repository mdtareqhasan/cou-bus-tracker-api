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
    private String studentId;
    private String department;
    private String varsityBatch;
    private String idCardImageUrl;
    private Boolean isEduMail;
    private Boolean isVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
