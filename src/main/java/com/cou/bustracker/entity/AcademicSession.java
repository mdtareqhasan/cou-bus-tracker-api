package com.cou.bustracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One selectable academic session, e.g. {@code 2020-2021}.
 *
 * <p>{@code name_en} is the canonical value older app versions stored
 * verbatim (ASCII, matching the backend's session format); {@code name_bn}
 * is the same range rendered in Bengali digits for display.
 *
 * <p>Seeded from the Flutter session options. Both language forms are
 * stored so the client renders whichever matches its locale.
 */
@Entity
@Table(name = "academic_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Bengali digits, e.g. '২০২০-২০২১'. */
    @Column(name = "name_bn", nullable = false, unique = true, length = 20)
    private String nameBn;

    /** English form, e.g. '2020-2021' — the canonical stored value. */
    @Column(name = "name_en", nullable = false, unique = true, length = 20)
    private String nameEn;

    /** Preserves the dropdown order the app shipped with. */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
