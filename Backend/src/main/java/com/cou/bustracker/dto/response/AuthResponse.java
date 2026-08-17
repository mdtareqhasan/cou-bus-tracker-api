package com.cou.bustracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private String role;

    // User details returned after login/register
    private Long id;
    private String name;
    private String email;
    private Boolean isVerified;
    private Boolean isEmailVerified;
    private Boolean isEduMail;
}
