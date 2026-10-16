package com.cou.bustracker.dto.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigUpdateRequest {

    /**
     * Map of config_key -> config_value. Keys must already exist in app_config.
     * Use /api/super-admin/config to view current keys.
     */
    @NotEmpty(message = "At least one config value must be provided")
    private Map<String, String> values;
}