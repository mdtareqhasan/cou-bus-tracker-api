package com.cou.bustracker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_otps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationOtp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String email;
    @Column(name = "user_role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserRole userRole;
    @Column(name = "otp_hash", nullable = false)
    private String otpHash;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;
    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default private Integer failedAttempts = 0;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    public enum UserRole { STUDENT, TEACHER }
}
