package com.cou.bustracker.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminProfileResponse {
    private Long id;
    private String name;
    private String email;
}
