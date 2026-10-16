package com.cou.bustracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracker_links")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackerLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", unique = true, nullable = false)
    private Bus bus;

    @Column(name = "tracker_url", nullable = false, columnDefinition = "TEXT")
    private String trackerUrl;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
