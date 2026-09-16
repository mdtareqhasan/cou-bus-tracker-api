-- V20: Rename student_id to roll_number and varsity_batch to session
-- This migration aligns the database columns with the Comilla University ID card format

-- Rename student_id column to roll_number
ALTER TABLE students RENAME COLUMN student_id TO roll_number;

-- Rename varsity_batch column to session
ALTER TABLE students RENAME COLUMN varsity_batch TO session;

-- Update index name if it exists (PostgreSQL auto-renames indexes on column rename)
-- The unique constraint on roll_number is automatically handled by Flyway

-- Add comment for clarity
COMMENT ON COLUMN students.roll_number IS 'University roll number from ID card (e.g., 12208055)';
COMMENT ON COLUMN students.session IS 'Academic session from ID card (e.g., 2021-22)';
