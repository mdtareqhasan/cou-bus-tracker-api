-- V17: Add phone verification support
-- Add phone and is_phone_verified to students
ALTER TABLE students ADD COLUMN IF NOT EXISTS phone VARCHAR(20) UNIQUE;
ALTER TABLE students ADD COLUMN IF NOT EXISTS is_phone_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Add is_phone_verified to teachers
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS is_phone_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Create phone verification OTPs table
CREATE TABLE IF NOT EXISTS phone_verification_otps (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    user_role VARCHAR(20) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    last_sent_at TIMESTAMP NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_phone_verification_otps_phone_role UNIQUE (phone, user_role)
);
