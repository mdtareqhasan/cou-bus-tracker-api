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
public class NoticeResponse {

    private Long id;
    private String title;
    private String body;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
