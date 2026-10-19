package com.cou.bustracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "teachers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Null for Google-only accounts; set for e-mail-registered accounts. */
    @Column
    private String password;

    @Column(name = "teacher_id", unique = true, length = 50)
    private String teacherId;

    @Column(length = 150)
    private String email;

    @Column(name = "is_email_verified")
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "id_card_image_url", columnDefinition = "TEXT")
    private String idCardImageUrl;

    @Column(name = "google_subject", unique = true, length = 255)
    private String googleSubject;

    @Column(name = "designation", length = 100)
    private String designation;

    /**
     * Where the teacher works — either a {@code DEPARTMENT} unit (teaching
     * staff) or an {@code OFFICE} unit (administrative staff). Null until
     * academic details are completed — Google Sign-In creates the row first.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private AcademicUnit department;

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
