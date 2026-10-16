package com.cou.bustracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One FCM registration token per user device.
 *
 * <p>Schema is owned by Flyway ({@code V22__create_user_fcm_tokens.sql});
 * {@code ddl-auto} is {@code none}, so this entity only maps to the existing
 * table. Constraints (the {@code user_type} CHECK and the unique indexes on
 * {@code token} and {@code (user_type, user_id, device_id)}) live in the
 * migration, not here.
 */
@Entity
@Table(name = "user_fcm_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 'STUDENT' or 'TEACHER' — enforced by the migration CHECK constraint. */
    @Column(name = "user_type", nullable = false, length = 20)
    private String userType;

    /** Id of the owning student or teacher (polymorphic — no FK by design). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    private String token;

    /** e.g. 'android' / 'ios'. Optional. */
    @Column(name = "platform", length = 30)
    private String platform;

    /** Optional app-supplied device identifier, used to key upserts. */
    @Column(name = "device_id", length = 150)
    private String deviceId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_seen_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastSeenAt = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
