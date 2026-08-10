package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTrackerLinkRequest {

    @NotBlank(message = "Tracker URL is required")
    private String trackerUrl;

    private LocalDateTime expiresAt;
}
