package com.cou.bustracker.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public, unauthenticated view of app configuration consumed by the Flutter app.
 * Returned from GET /api/config.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicConfigResponse {

    private String apiBaseUrl;
    private String latestAppVersion;
    private String minimumAppVersion;
    private Boolean forceUpdate;
    private String updateMessage;
    private String playStoreUrl;
    private Boolean maintenanceMode;
    private String maintenanceMessage;
}