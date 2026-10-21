package com.cou.bustracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One route as the rider has configured it. Merged client-side against the
 * public {@code /api/routes} listing by {@code routeId} — this response stays
 * free of route names so there is exactly one source of truth for them.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePreferenceResponse {

    private Long routeId;
    private Boolean isPinned;
    private Boolean scheduleAlertsEnabled;
}
