-- V13: Fix boolean columns (DBeaver imported MySQL TINYINT(1) as INTEGER)
-- Convert INTEGER columns to proper PostgreSQL BOOLEAN.

-- BUSES
ALTER TABLE buses ALTER COLUMN is_active TYPE BOOLEAN USING is_active::BOOLEAN;
ALTER TABLE buses ALTER COLUMN is_active SET DEFAULT true;

-- SCHEDULES
ALTER TABLE schedules ALTER COLUMN is_active TYPE BOOLEAN USING is_active::BOOLEAN;
ALTER TABLE schedules ALTER COLUMN is_active SET DEFAULT true;

-- NOTICES
ALTER TABLE notices ALTER COLUMN is_active TYPE BOOLEAN USING is_active::BOOLEAN;
ALTER TABLE notices ALTER COLUMN is_active SET DEFAULT true;

-- STUDENTS
ALTER TABLE students ALTER COLUMN is_edu_mail TYPE BOOLEAN USING is_edu_mail::BOOLEAN;
ALTER TABLE students ALTER COLUMN is_edu_mail SET DEFAULT false;
ALTER TABLE students ALTER COLUMN is_verified TYPE BOOLEAN USING is_verified::BOOLEAN;
ALTER TABLE students ALTER COLUMN is_verified SET DEFAULT false;
ALTER TABLE students ALTER COLUMN is_active TYPE BOOLEAN USING is_active::BOOLEAN;
ALTER TABLE students ALTER COLUMN is_active SET DEFAULT true;

-- TEACHERS
ALTER TABLE teachers ALTER COLUMN is_edu_mail TYPE BOOLEAN USING is_edu_mail::BOOLEAN;
ALTER TABLE teachers ALTER COLUMN is_edu_mail SET DEFAULT false;
ALTER TABLE teachers ALTER COLUMN is_verified TYPE BOOLEAN USING is_verified::BOOLEAN;
ALTER TABLE teachers ALTER COLUMN is_verified SET DEFAULT false;
ALTER TABLE teachers ALTER COLUMN is_active TYPE BOOLEAN USING is_active::BOOLEAN;
ALTER TABLE teachers ALTER COLUMN is_active SET DEFAULT true;
