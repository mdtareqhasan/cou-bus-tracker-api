-- =====================================================================
-- V21: switch registration verification from phone to email
-- =====================================================================

-- 1) students: add email + email-verified flag
ALTER TABLE students
    ADD COLUMN email VARCHAR(150),
    ADD COLUMN is_email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- 2) teachers: same
ALTER TABLE teachers
    ADD COLUMN email VARCHAR(150),
    ADD COLUMN is_email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Unique only when present (existing users have no email yet).
-- The application must store emails lowercased.
CREATE UNIQUE INDEX uk_students_email ON students (email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uk_teachers_email ON teachers (email) WHERE email IS NOT NULL;

-- 3) phone is no longer required for new registrations
ALTER TABLE students ALTER COLUMN phone DROP NOT NULL;
ALTER TABLE teachers ALTER COLUMN phone DROP NOT NULL;

-- 4) email OTP table: add the same staging columns V19 added for phone
ALTER TABLE email_verification_otps
    ADD COLUMN pending_registration_json TEXT,
    ADD COLUMN pending_id_card_url TEXT;

-- 5) drop leftover rows from the old dummy email flow; they have no pending payload
DELETE FROM email_verification_otps;