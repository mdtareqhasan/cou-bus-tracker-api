package com.cou.bustracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private String message;
    /** Optional machine-readable marker (e.g. OTP_SENT) clients can localize on. */
    private String code;
    private Long id;
    private String busNumber;
    private String expiresAt;
}
