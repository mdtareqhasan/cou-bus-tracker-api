-- V19: Defer Student/Teacher creation until OTP verification succeeds.
-- Add payload + ID-card URL columns to phone_verification_otps so the
-- full registration form (name, password, studentId/teacherId, etc.)
-- and the uploaded ID-card can be staged before OTP verify.
-- If the user never verifies, the row (and eventually the Cloudinary
-- upload) is reaped by PhoneVerificationService.cleanupExpired().
ALTER TABLE phone_verification_otps
    ADD COLUMN IF NOT EXISTS pending_registration_json TEXT,
    ADD COLUMN IF NOT EXISTS pending_id_card_url TEXT;
