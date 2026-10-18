package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNoticeRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Body is required")
    private String body;

    /**
     * Who the notice is for. Case-insensitive on the wire ("student" works);
     * stored uppercase. Null/blank falls back to ALL so older clients that
     * don't send the field keep their broadcast behavior.
     */
    @Pattern(regexp = "(?i)STUDENT|TEACHER|ALL",
            message = "Audience must be STUDENT, TEACHER or ALL")
    private String audience = "ALL";

    private Integer expiryHours;
}
