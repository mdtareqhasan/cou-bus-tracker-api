package com.cou.bustracker.repository;

import com.cou.bustracker.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    /**
     * Every active token for a whole user type — the broadcast audience used
     * when a schedule changes (e.g. all students, all teachers).
     */
    List<UserFcmToken> findByUserTypeAndIsActiveTrue(String userType);

    /**
     * Active tokens for a <em>subset</em> of one user type — the audience when
     * only riders who follow a particular route care about a change.
     */
    List<UserFcmToken> findByUserTypeAndUserIdInAndIsActiveTrue(String userType, Collection<Long> userIds);

    /** Bulk delete by token — used to prune tokens FCM reports as unregistered. */
    long deleteByTokenIn(Collection<String> tokens);

    /** Ownership-checked delete by token; returns the number of rows removed. */
    long deleteByUserTypeAndUserIdAndToken(String userType, Long userId, String token);

    /** Remove every token for a user (e.g. "log out everywhere"). */
    long deleteByUserTypeAndUserId(String userType, Long userId);

    boolean existsByUserTypeAndUserIdAndToken(String userType, Long userId, String token);
}
