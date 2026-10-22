package com.cou.bustracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Public status from GET /api/health: "ok" or "down" per component. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {

    /** Always "ok" — the API answered this request. */
    private String api;

    /** "ok" when the DB health check passes, "down" otherwise. */
    private String db;
}
