package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.RegisterFcmTokenRequest;
import com.cou.bustracker.entity.UserFcmToken;
import com.cou.bustracker.repository.UserFcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores and removes FCM registration tokens for the authenticated user.
 *
 * <p>Only STUDENT and TEACHER tokens are accepted — that is the scope of the
 * {@code user_type} CHECK constraint in {@code V22__create_user_fcm_tokens.sql}.
 * Admin / super-admin callers get a clear 400 instead of a raw Postgres
 * constraint violation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenService {

    /** Roles the schema allows in user_fcm_tokens.user_type. */
    private static final Set<String> SUPPORTED_ROLES = Set.of("STUDENT", "TEACHER");

    /**
     * Languages pushes can be rendered in. Must match the locales the Flutter
     * app supports (lib/l10n) — a device asking for anything else gets the
     * fallback language so a stray tag can never produce a half-translated
     * notification.
     */
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("bn", "en");

    /** Language used when a device has no usable language of its own. */
    public static final String FALLBACK_LANGUAGE = "en";

    private final UserFcmTokenRepository userFcmTokenRepository;

    /**
     * Upserts the caller's device token.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Match by (userType, userId, deviceId) when the client sends a
     *       deviceId — FCM rotates tokens, so the same device getting a new
     *       token overwrites its row rather than inserting a duplicate.</li>
     *   <li>Otherwise match by the globally-unique token.</li>
     * </ol>
     *
     * @return the saved token row
     * @throws IllegalArgumentException if the caller's role isn't supported
     */
    @Transactional
    public UserFcmToken register(CurrentUser user, RegisterFcmTokenRequest request) {
        if (!SUPPORTED_ROLES.contains(user.role())) {
            throw new IllegalArgumentException(
                    "FCM tokens are only supported for students and teachers");
        }

        String token = request.getToken().trim();
        String deviceId = blankToNull(request.getDeviceId());
        String platform = blankToNull(request.getPlatform());

        UserFcmToken entity = findByDevice(user, deviceId)
                .or(() -> userFcmTokenRepository.findByToken(token))
                .orElseGet(() -> UserFcmToken.builder()
                        .userType(user.role())
                        .userId(user.userId())
                        .build());

        entity.setToken(token);
        entity.setPlatform(platform);
        entity.setDeviceId(deviceId);
        entity.setAppLanguage(resolveLanguage(request.getAppLanguage(), entity.getAppLanguage()));
        entity.setIsActive(true);
        entity.setLastSeenAt(LocalDateTime.now());

        UserFcmToken saved = userFcmTokenRepository.save(entity);
        log.info("FCM token registered for {} {} (row id {})",
                user.role(), user.userId(), saved.getId());
        return saved;
    }

    /**
     * Deletes the given token, but only if it belongs to the caller. Deleting a
     * token the caller doesn't own is a no-op (0 rows) so this endpoint can't be
     * used to strip someone else's device.
     *
     * @return true if a token was removed
     */
    @Transactional
    public boolean unregister(CurrentUser user, String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }
        long deleted = userFcmTokenRepository
                .deleteByUserTypeAndUserIdAndToken(user.role(), user.userId(), token.trim());
        if (deleted > 0) {
            log.info("FCM token removed for {} {} ({} row(s))", user.role(), user.userId(), deleted);
        }
        return deleted > 0;
    }

    /**
     * Removes every token for the caller (e.g. "log out everywhere").
     *
     * @return the number of tokens removed
     */
    @Transactional
    public long unregisterAll(CurrentUser user) {
        long deleted = userFcmTokenRepository.deleteByUserTypeAndUserId(user.role(), user.userId());
        log.info("All FCM tokens removed for {} {} ({} row(s))", user.role(), user.userId(), deleted);
        return deleted;
    }

    /** Active tokens to target when sending a push to this user. */
    @Transactional(readOnly = true)
    public List<UserFcmToken> getActiveTokens(String userType, Long userId) {
        return userFcmTokenRepository.findByUserTypeAndUserIdAndIsActiveTrue(userType, userId);
    }

    /**
     * Every active registration token of a user type, grouped by the app
     * language it should be rendered in — the broadcast audience for schedule
     * notifications (all students / all teachers). The push dispatcher renders
     * one localized message per group and fans out separately, so a Bengali
     * rider gets Bengali text and an English rider gets English.
     *
     * <p>Tokens with a missing or unsupported {@code app_language} land under
     * {@link #FALLBACK_LANGUAGE}, so every device is always in exactly one
     * group.
     *
     * @return language → raw tokens, in storage order
     */
    @Transactional(readOnly = true)
    public Map<String, List<String>> getActiveTokensGroupedByLanguage(String userType) {
        if (!SUPPORTED_ROLES.contains(userType)) {
            throw new IllegalArgumentException("Unsupported FCM audience: " + userType);
        }
        return groupByLanguage(userFcmTokenRepository.findByUserTypeAndIsActiveTrue(userType));
    }

    /**
     * As {@link #getActiveTokensGroupedByLanguage(String)}, but restricted to
     * specific users of one type — the audience when only riders who follow a
     * route (per their saved route preferences) care about a change.
     *
     * <p>Empty {@code userIds} short-circuits to an empty map: a route nobody
     * follows has an empty audience, not a broadcast.
     */
    @Transactional(readOnly = true)
    public Map<String, List<String>> getActiveTokensGroupedByLanguage(String userType, Collection<Long> userIds) {
        if (!SUPPORTED_ROLES.contains(userType)) {
            throw new IllegalArgumentException("Unsupported FCM audience: " + userType);
        }
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return groupByLanguage(userFcmTokenRepository.findByUserTypeAndUserIdInAndIsActiveTrue(userType, userIds));
    }

    private static Map<String, List<String>> groupByLanguage(List<UserFcmToken> tokens) {
        Map<String, List<String>> byLanguage = new LinkedHashMap<>();
        for (UserFcmToken token : tokens) {
            byLanguage.computeIfAbsent(
                    resolveLanguage(token.getAppLanguage(), null), k -> new ArrayList<>())
                    .add(token.getToken());
        }
        return byLanguage;
    }

    /**
     * Removes tokens FCM has permanently rejected as unregistered, so the
     * audience stays clean instead of accumulating undeliverable devices.
     * Called from the FCM result callback; safe to invoke from any thread.
     *
     * @return number of rows deleted
     */
    @Transactional
    public long pruneUnregisteredTokens(Collection<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return 0L;
        }
        long removed = userFcmTokenRepository.deleteByTokenIn(tokens);
        if (removed > 0) {
            log.info("Pruned {} unregistered FCM token(s)", removed);
        }
        return removed;
    }

    private java.util.Optional<UserFcmToken> findByDevice(CurrentUser user, String deviceId) {
        if (deviceId == null) {
            return java.util.Optional.empty();
        }
        return userFcmTokenRepository.findByUserTypeAndUserIdAndDeviceId(
                user.role(), user.userId(), deviceId);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /**
     * Normalizes a client-supplied language tag to a supported language code:
     * "bn", "bn-BD", "BN" → "bn"; "en-US" → "en". Anything unsupported or
     * blank falls back to {@code currentOrNull} (the value already stored on
     * the row) and finally to {@link #FALLBACK_LANGUAGE}, so an older client
     * that doesn't send a language can't wipe the one we already know.
     */
    private static String resolveLanguage(String requested, String currentOrNull) {
        String requestedTrimmed = blankToNull(requested);
        if (requestedTrimmed != null) {
            String base = requestedTrimmed.split("[-_]", 2)[0].toLowerCase();
            if (SUPPORTED_LANGUAGES.contains(base)) {
                return base;
            }
            // An unsupported tag is a real (if odd) choice — don't keep stale state.
            return FALLBACK_LANGUAGE;
        }
        String current = blankToNull(currentOrNull);
        if (current != null && SUPPORTED_LANGUAGES.contains(current)) {
            return current;
        }
        return FALLBACK_LANGUAGE;
    }
}
