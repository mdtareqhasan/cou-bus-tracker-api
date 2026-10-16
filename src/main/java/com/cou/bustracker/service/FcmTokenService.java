package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.RegisterFcmTokenRequest;
import com.cou.bustracker.entity.UserFcmToken;
import com.cou.bustracker.repository.UserFcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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
     * Every active registration token of a user type — the broadcast audience
     * for schedule notifications (all students / all teachers).
     *
     * @return the raw tokens, in storage order
     */
    @Transactional(readOnly = true)
    public List<String> getActiveTokensForRole(String userType) {
        if (!SUPPORTED_ROLES.contains(userType)) {
            throw new IllegalArgumentException("Unsupported FCM audience: " + userType);
        }
        return userFcmTokenRepository.findByUserTypeAndIsActiveTrue(userType)
                .stream()
                .map(UserFcmToken::getToken)
                .toList();
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
}
