package com.cou.bustracker.repository;

import com.cou.bustracker.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

    /** Global unique token. */
    Optional<UserFcmToken> findByToken(String token);

    /**
     * Device-scoped lookup. Backed by the partial unique index
     * {@code uk_user_fcm_tokens_device ... WHERE device_id IS NOT NULL}, so at
     * most one row matches when {@code deviceId} is non-null.
     */
    Optional<UserFcmToken> findByUserTypeAndUserIdAndDeviceId(String userType, Long userId, String deviceId);

    /** Active tokens for a user — what you'd use when sending a push. */
    List<UserFcmToken> findByUserTypeAndUserIdAndIsActiveTrue(String userType, Long userId);

    /** Ownership-checked delete by token; returns the number of rows removed. */
    long deleteByUserTypeAndUserIdAndToken(String userType, Long userId, String token);

    /** Remove every token for a user (e.g. "log out everywhere"). */
    long deleteByUserTypeAndUserId(String userType, Long userId);

    boolean existsByUserTypeAndUserIdAndToken(String userType, Long userId, String token);
}
