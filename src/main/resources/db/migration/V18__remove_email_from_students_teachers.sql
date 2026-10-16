-- V18: Remove email-related columns from students and teachers (phone-only auth)

-- Clean up existing data first
DELETE FROM students;
DELETE FROM teachers;
DELETE FROM phone_verification_otps;
DELETE FROM email_verification_otps;

-- Students
ALTER TABLE students DROP COLUMN IF EXISTS email;
ALTER TABLE students DROP COLUMN IF EXISTS is_email_verified;
ALTER TABLE students DROP COLUMN IF EXISTS is_edu_mail;
ALTER TABLE students ALTER COLUMN phone SET NOT NULL;
-- Ensure phone is unique (already added in V17, but ensure)
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_students_phone') THEN
    ALTER TABLE students ADD CONSTRAINT uk_students_phone UNIQUE (phone);
  END IF;
END $$;

-- Teachers
ALTER TABLE teachers DROP COLUMN IF EXISTS email;
ALTER TABLE teachers DROP COLUMN IF EXISTS is_email_verified;
ALTER TABLE teachers DROP COLUMN IF EXISTS is_edu_mail;
ALTER TABLE teachers ALTER COLUMN phone SET NOT NULL;
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_teachers_phone') THEN
    ALTER TABLE teachers ADD CONSTRAINT uk_teachers_phone UNIQUE (phone);
  END IF;
END $$;
