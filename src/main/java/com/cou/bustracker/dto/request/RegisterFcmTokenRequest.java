package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for {@code POST /api/notifications/fcm-token}.
 *
 * <p>The user (type + id) is resolved from the JWT, never trusted from the
 * body — only the token and optional device metadata come from the client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterFcmTokenRequest {

    @NotBlank(message = "Token is required")
    @Size(max = 4096, message = "Token exceeds the allowed length")
    private String token;

    /** Optional: 'android' / 'ios'. */
    @Size(max = 30, message = "Platform must be at most 30 characters")
    private String platform;

    /**
     * Optional stable per-install id. When present it keys the upsert, so a
     * rotated FCM token for the same device overwrites the old row instead of
     * inserting a duplicate.
     */
    @Size(max = 150, message = "Device ID must be at most 150 characters")
    private String deviceId;

    /**
     * Optional app language ('bn' / 'en'). Push notifications are rendered in
     * this language for this device; unsupported or missing values fall back
     * to English.
     */
    @Size(max = 10, message = "Language must be at most 10 characters")
    private String appLanguage;
}
