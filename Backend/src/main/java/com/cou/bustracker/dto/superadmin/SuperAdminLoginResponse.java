package com.cou.bustracker.dto.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminLoginResponse {

    private String token;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private SuperAdminResponse superAdmin;
}