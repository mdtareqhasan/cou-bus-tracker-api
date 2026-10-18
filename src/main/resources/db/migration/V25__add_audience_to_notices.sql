-- =====================================================================
-- V25: notice audience — who a notice is meant for
--
-- Reuses the user_type vocabulary from user_fcm_tokens (STUDENT/TEACHER)
-- plus ALL, so the push fan-out can map an audience straight onto FCM
-- token groups. Existing rows default to ALL: they were visible to
-- everyone before this column existed.
-- =====================================================================

ALTER TABLE notices
    ADD COLUMN IF NOT EXISTS audience VARCHAR(20) NOT NULL DEFAULT 'ALL';

ALTER TABLE notices
    DROP CONSTRAINT IF EXISTS chk_notices_audience;

ALTER TABLE notices
    ADD CONSTRAINT chk_notices_audience
        CHECK (audience IN ('STUDENT', 'TEACHER', 'ALL'));
