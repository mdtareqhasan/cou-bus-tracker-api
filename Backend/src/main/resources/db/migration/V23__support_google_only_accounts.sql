-- =====================================================================
-- V23: support Google-only accounts
-- =====================================================================
-- Google sign-in verifies an institutional email and may create a pending
-- Student/Teacher row before the user has supplied academic details or a
-- password. Those columns therefore become optional. The OTP registration
-- flow still populates every one of them.

-- password: already nullable since V14, repeated here so a fresh database
-- built from scratch ends up in the same state.
ALTER TABLE students ALTER COLUMN password DROP NOT NULL;
ALTER TABLE teachers ALTER COLUMN password DROP NOT NULL;

-- academic details are collected/verified later by the user or admin
ALTER TABLE students ALTER COLUMN roll_number DROP NOT NULL;
ALTER TABLE students ALTER COLUMN department DROP NOT NULL;
ALTER TABLE students ALTER COLUMN session DROP NOT NULL;
