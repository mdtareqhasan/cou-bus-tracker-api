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

    @NotBlank(message = "টোকেন দিন")
    @Size(max = 4096, message = "টোকেন অনুমোদিত দৈর্ঘ্যের বেশি হতে পারবে না")
    private String token;

    /** Optional: 'android' / 'ios'. */
    @Size(max = 30, message = "প্ল্যাটফর্ম ৩০ অক্ষরের বেশি হতে পারবে না")
    private String platform;

    /**
     * Optional stable per-install id. When present it keys the upsert, so a
     * rotated FCM token for the same device overwrites the old row instead of
     * inserting a duplicate.
     */
    @Size(max = 150, message = "ডিভাইস আইডি ১৫০ অক্ষরের বেশি হতে পারবে না")
    private String deviceId;
}
