package com.cou.bustracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public, unauthenticated service status returned by GET /api/health.
 *
 * Flat by design: an uptime monitor or a human curling the endpoint should
 * read api/database health and the admin-frontend URLs without drilling
 * into nested structures.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {

    /** Overall status: UP only when the API answered AND the DB check passed. */
    private String status;

    /** When this snapshot was taken (UTC, ISO-8601). */
    private String timestamp;

    /** Always UP if this response exists — the API answered the request. */
    private String api;

    /** UP / DOWN / UNKNOWN, from Spring Boot Actuator's DataSource indicator. */
    private String database;

    /** URL of the admin panel frontend. */
    private String adminPanel;

    /** URL of the super-admin panel frontend. */
    private String superAdminPanel;
}
