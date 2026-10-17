package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.RegisterFcmTokenRequest;
import com.cou.bustracker.entity.UserFcmToken;
import com.cou.bustracker.repository.UserFcmTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins down how the device's app language is stored and grouped — it decides
 * which language bundle a rider's push is rendered from.
 */
class FcmTokenServiceTest {

    private static final CurrentUser STUDENT = new CurrentUser(10L, "STUDENT", "s@couniv.edu");

    private UserFcmTokenRepository repository;
    private FcmTokenService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserFcmTokenRepository.class);
        service = new FcmTokenService(repository);
        when(repository.save(any(UserFcmToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registerNormalizesLanguageTagToBaseCode() {
        when(repository.findByToken("tok")).thenReturn(Optional.empty());

        UserFcmToken saved = service.register(STUDENT, request("tok", "bn-BD"));

        assertEquals("bn", saved.getAppLanguage());
    }

    @Test
    void registerKeepsExistingLanguageWhenClientOmitsIt() {
        UserFcmToken existing = token("tok", "bn");
        when(repository.findByToken("tok")).thenReturn(Optional.of(existing));

        // An older app version doesn't send appLanguage — it must not wipe the
        // language we already know for this device.
        UserFcmToken saved = service.register(STUDENT, request("tok", null));

        assertEquals("bn", saved.getAppLanguage());
    }

    @Test
    void registerFallsBackToEnglishForNewDeviceWithoutLanguage() {
        when(repository.findByToken("tok")).thenReturn(Optional.empty());

        UserFcmToken saved = service.register(STUDENT, request("tok", null));

        assertEquals("en", saved.getAppLanguage());
    }

    @Test
    void registerFallsBackToEnglishForUnsupportedLanguage() {
        when(repository.findByToken("tok")).thenReturn(Optional.empty());

        UserFcmToken saved = service.register(STUDENT, request("tok", "fr"));

        assertEquals("en", saved.getAppLanguage());
    }

    @Test
    void groupsActiveTokensByLanguageWithEnglishFallback() {
        when(repository.findByUserTypeAndIsActiveTrue("STUDENT")).thenReturn(List.of(
                token("t1", "bn"),
                token("t2", "en"),
                token("t3", null),    // legacy row, never sent a language
                token("t4", "fr")));  // unsupported tag

        Map<String, List<String>> byLanguage = service.getActiveTokensGroupedByLanguage("STUDENT");

        assertEquals(Map.of(
                "bn", List.of("t1"),
                "en", List.of("t2", "t3", "t4")), byLanguage);
    }

    private static RegisterFcmTokenRequest request(String token, String appLanguage) {
        RegisterFcmTokenRequest request = new RegisterFcmTokenRequest();
        request.setToken(token);
        request.setAppLanguage(appLanguage);
        return request;
    }

    private static UserFcmToken token(String token, String appLanguage) {
        return UserFcmToken.builder()
                .userType("STUDENT")
                .userId(10L)
                .token(token)
                .appLanguage(appLanguage)
                .build();
    }
}
