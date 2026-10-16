package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNoticeRequest {

    @NotBlank(message = "শিরোনাম দিন")
    private String title;

    @NotBlank(message = "বডি লিখুন")
    private String body;

    private Integer expiryHours;
}
