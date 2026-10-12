-- =====================================================================
-- V22: store Firebase Cloud Messaging tokens per user device
-- =====================================================================

CREATE TABLE IF NOT EXISTS user_fcm_tokens (
    id            BIGSERIAL    PRIMARY KEY,
    user_type     VARCHAR(20)  NOT NULL,
    user_id       BIGINT       NOT NULL,
    token         TEXT         NOT NULL,
    platform      VARCHAR(30),
    device_id     VARCHAR(150),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    last_seen_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_user_fcm_tokens_user_type
        CHECK (user_type IN ('STUDENT', 'TEACHER')),
    CONSTRAINT uk_user_fcm_tokens_token
        UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_user_fcm_tokens_user
    ON user_fcm_tokens(user_type, user_id);

CREATE INDEX IF NOT EXISTS idx_user_fcm_tokens_active_user
    ON user_fcm_tokens(user_type, user_id, is_active);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_fcm_tokens_device
    ON user_fcm_tokens(user_type, user_id, device_id)
    WHERE device_id IS NOT NULL;
