package com.cou.bustracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, length = 20)
    private String phone;

    /** Null for Google-only accounts; set for OTP-registered accounts. */
    @Column
    private String password;

    @Column(name = "google_subject", unique = true, length = 255)
    private String googleSubject;

    /** Filled in after a Google account is verified by the admin; null until then. */
    @Column(name = "roll_number", length = 50)
    private String rollNumber;

    @Column(length = 100)
    private String department;

    @Column(name = "session", length = 20)
    private String session;

    @Column(length = 150)
    private String email;

    @Column(name = "is_email_verified")
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "id_card_image_url", columnDefinition = "TEXT")
    private String idCardImageUrl;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_phone_verified")
    @Builder.Default
    private Boolean isPhoneVerified = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
