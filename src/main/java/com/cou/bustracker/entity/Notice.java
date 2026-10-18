package com.cou.bustracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /**
     * Who this notice is for — STUDENT / TEACHER / ALL. Stored uppercase to
     * match the {@code user_type} vocabulary in {@code user_fcm_tokens}, so a
     * future push fan-out can map the audience straight onto token groups.
     */
    @Column(name = "audience", nullable = false, length = 20)
    @Builder.Default
    private String audience = "ALL";

    @Column(name = "expiry_hours")
    @Builder.Default
    private Integer expiryHours = 24;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
