package com.cou.bustracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_bn", unique = true, nullable = false, length = 255)
    private String nameBn;

    @Column(name = "name_en", unique = true, nullable = false, length = 255)
    private String nameEn;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * The other physical direction of this corridor, e.g. {@code "Kandirpar →
     * Campus"} ↔ {@code "Campus → Kandirpar"}. One row = one direction, so a
     * rider's round trip is two rows paired here.
     *
     * <p>Stored as a bare id rather than a {@code @ManyToOne} because the only
     * consumer (the public route listing) already has both rows in hand and
     * just needs the id to draw the pair — an association would buy a lazy
     * proxy nobody reads. Reciprocity (A.opposite = B ⇒ B.opposite = A) is
     * enforced in {@code RouteService} plus DB guardrails (no self-opposite
     * CHECK, UNIQUE index so a route can be the opposite of at most one other).
     */
    @Column(name = "opposite_route_id")
    private Long oppositeRouteId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
