package com.cou.bustracker.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for {@code PUT /api/me/route-preferences/{routeId}}.
 *
 * <p>Both fields are optional; a null field means "leave this flag alone", so
 * the app can flip just the pin or just the bell without echoing state it
 * already holds. A request that would leave both flags false is treated as a
 * delete (see {@code UserRoutePreferenceService}) — a row with neither flag
 * set carries no information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutePreferenceRequest {

    private Boolean isPinned;

    private Boolean scheduleAlertsEnabled;
}
