package com.cou.bustracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One rider's relationship with one route: is it pinned on their home screen,
 * and do they want a push when its schedules change.
 *
 * <p>Stored on the server (rather than device-local) for two reasons: pins
 * follow the rider across phones, and {@code schedule_alerts_enabled} is the
 * audience the push dispatcher targets — a device-local flag could never scope
 * a notification.
 *
 * <p>The two flags are deliberately independent: a rider may want a route
 * pinned without being buzzed about every timetable tweak, or quietly follow
 * a route they don't commute on. A row where both are false carries no
 * information, so the service deletes it rather than storing it.
 */
@Entity
@Table(name = "user_route_preferences",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_route_pref",
                columnNames = {"user_type", "user_id", "route_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoutePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** STUDENT or TEACHER — same polymorphic key as {@code user_fcm_tokens}. */
    @Column(name = "user_type", nullable = false, length = 20)
    private String userType;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    /** Pinned to the top of the rider's home screen. */
    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    /**
     * Opt-in for per-route schedule-change pushes. Off by default — a push a
     * rider never asked for is the fastest route to them muting the app.
     */
    @Column(name = "schedule_alerts_enabled", nullable = false)
    @Builder.Default
    private Boolean scheduleAlertsEnabled = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
