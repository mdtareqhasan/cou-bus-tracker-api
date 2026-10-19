package com.cou.bustracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One selectable academic unit — either a teaching department
 * ({@code DEPARTMENT}, e.g. 'গণিত') or an administrative office
 * ({@code OFFICE}, e.g. 'রেজিস্ট্রারের কার্যালয়').
 *
 * <p>Departments and offices share a table because a teacher works in
 * exactly one of either kind: the app's teacher picker is literally
 * {@code departmentOptions + officeOptions}. Students are restricted to
 * {@code DEPARTMENT} rows; teachers may pick either.
 *
 * <p>Seeded from the Flutter option files. Both language forms are stored
 * so the client renders whichever matches its locale — the server never
 * picks a language.
 */
@Entity
@Table(name = "academic_units")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@code DEPARTMENT} or {@code OFFICE}. */
    @Column(name = "unit_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UnitType unitType;

    /** Bengali name — the canonical value older app versions stored verbatim. */
    @Column(name = "name_bn", nullable = false, length = 200)
    private String nameBn;

    /** English display label. */
    @Column(name = "name_en", nullable = false, length = 200)
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

    public enum UnitType { DEPARTMENT, OFFICE }
}
