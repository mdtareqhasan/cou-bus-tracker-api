package com.cou.bustracker.service;

import com.cou.bustracker.dto.config.PublicConfigResponse;
import com.cou.bustracker.entity.AppConfig;
import com.cou.bustracker.repository.AppConfigRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract the Flutter update prompt relies on: `/api/config` must
 * expose both language variants so an English-locale rider never sees the
 * Bangla message (and vice versa), while the legacy single message keeps
 * working for clients that have not updated yet.
 */
class AppConfigServiceTest {

    private final AppConfigRepository repository = mock(AppConfigRepository.class);
    private final AppConfigService service = new AppConfigService(repository);

    @Test
    void publicConfigExposesBothLanguageVariants() {
        when(repository.findAll()).thenReturn(List.of(
                config("update_available_message", "পুরনো বার্তা"),
                config("update_available_message_bn", "নতুন বাংলা বার্তা"),
                config("update_available_message_en", "New English message")));
        service.reloadCache();

        PublicConfigResponse response = service.getPublicConfig();

        assertEquals("পুরনো বার্তা", response.getUpdateMessage(),
                "legacy key must stay untouched for old clients");
        assertEquals("নতুন বাংলা বার্তা", response.getUpdateMessageBn());
        assertEquals("New English message", response.getUpdateMessageEn());
    }

    @Test
    void missingLanguageKeysFallBackToTheLegacyMessage() {
        when(repository.findAll()).thenReturn(List.of(
                config("update_available_message", "Legacy message")));
        service.reloadCache();

        PublicConfigResponse response = service.getPublicConfig();

        assertEquals("Legacy message", response.getUpdateMessageBn());
        assertEquals("Legacy message", response.getUpdateMessageEn());
    }

    private AppConfig config(String key, String value) {
        return AppConfig.builder().configKey(key).configValue(value).build();
    }
}
