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

    /** Null for Google-only accounts; set for e-mail-registered accounts. */
    @Column
    private String password;

    @Column(name = "google_subject", unique = true, length = 255)
    private String googleSubject;

    /** Filled in after a Google account is verified by the admin; null until then. */
    @Column(name = "roll_number", length = 50)
    private String rollNumber;

    /**
     * The student's department (always a {@code DEPARTMENT} unit). Null until
     * academic details are completed — Google Sign-In creates the row first.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private AcademicUnit department;

    /** Null until academic details are completed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private AcademicSession session;

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

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
