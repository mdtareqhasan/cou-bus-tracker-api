package com.cou.bustracker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "phone_verification_otps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneVerificationOtp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String phone;
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
    /** Serialized registration payload (name, password hash, role-specific IDs, etc.).
     *  Populated by /api/auth/phone-verification/init; consumed by verifyOtp
     *  to create the Student or Teacher row ONLY on successful OTP entry. */
    @Column(name = "pending_registration_json", columnDefinition = "TEXT")
    private String pendingRegistrationJson;
    /** Cloudinary URL of the ID-card image uploaded at init time.
     *  Attached to the Student/Teacher row only if OTP verification succeeds. */
    @Column(name = "pending_id_card_url", columnDefinition = "TEXT")
    private String pendingIdCardUrl;
    public enum UserRole { STUDENT, TEACHER }
}
