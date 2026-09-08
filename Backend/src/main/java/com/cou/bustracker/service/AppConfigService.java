package com.cou.bustracker.service;

import com.cou.bustracker.dto.config.AppConfigUpdateRequest;
import com.cou.bustracker.dto.config.PublicConfigResponse;
import com.cou.bustracker.entity.AppConfig;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.AppConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppConfigService {

    private final AppConfigRepository appConfigRepository;

    /** In-memory cache for fast public reads (GET /api/config). */
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initCache() {
        log.info("Loading app_config into in-memory cache...");
        reloadCache();
    }

    public void reloadCache() {
        cache.clear();
        appConfigRepository.findAll().forEach(cfg -> cache.put(cfg.getConfigKey(), cfg.getConfigValue()));
        log.info("Loaded {} app_config entries", cache.size());
    }

    public String get(String key) {
        String value = cache.get(key);
        if (value == null) {
            // Lazy-load if not in cache
            value = appConfigRepository.findByConfigKey(key)
                    .map(AppConfig::getConfigValue)
                    .orElseThrow(() -> new ResourceNotFoundException("Config key not found: " + key));
            cache.put(key, value);
        }
        return value;
    }

    public String getOrDefault(String key, String defaultValue) {
        try {
            return get(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Boolean getBoolean(String key) {
        return Boolean.parseBoolean(getOrDefault(key, "false"));
    }

    public List<AppConfig> listAll() {
        return appConfigRepository.findAllByOrderByConfigKeyAsc();
    }

    @Transactional
    public List<AppConfig> updateConfig(AppConfigUpdateRequest request, String updatedBy) {
        Map<String, String> updates = request.getValues();
        List<AppConfig> updated = new java.util.ArrayList<>();

        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            AppConfig existing = appConfigRepository.findByConfigKey(key)
                    .orElseThrow(() -> new ResourceNotFoundException("Unknown config key: " + key));

            existing.setConfigValue(value);
            existing.setUpdatedBy(updatedBy);
            // @UpdateTimestamp handles updatedAt
            AppConfig saved = appConfigRepository.save(existing);
            updated.add(saved);

            // Invalidate cache
            cache.put(key, value);
        }

        log.info("Super admin '{}' updated {} config keys", updatedBy, updated.size());
        return updated;
    }

    public PublicConfigResponse getPublicConfig() {
        return PublicConfigResponse.builder()
                .apiBaseUrl(getOrDefault("api_base_url", "http://localhost:8080/api"))
                .latestAppVersion(getOrDefault("latest_app_version", "1.0.0"))
                .minimumAppVersion(getOrDefault("minimum_app_version", "1.0.0"))
                .forceUpdate(getBoolean("force_update"))
                .updateMessage(getOrDefault("update_available_message", "নতুন ভার্সন পাওয়া গেছে! আপডেট করুন।"))
                .playStoreUrl(getOrDefault("play_store_url", "https://play.google.com/store/apps/details?id=com.cou.bustracker"))
                .maintenanceMode(getBoolean("maintenance_mode"))
                .maintenanceMessage(getOrDefault("maintenance_message", "অ্যাপটি রক্ষণাবেক্ষণে আছে। শীঘ্রই ফিরে আসছি।"))
                .build();
    }
}