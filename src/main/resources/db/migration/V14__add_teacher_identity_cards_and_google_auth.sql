-- Additive migration only: safe for already deployed Flyway databases.
ALTER TABLE students ADD COLUMN IF NOT EXISTS google_subject VARCHAR(255);
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS teacher_id VARCHAR(50);
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS id_card_image_url TEXT;
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS google_subject VARCHAR(255);

ALTER TABLE students ALTER COLUMN password DROP NOT NULL;
ALTER TABLE teachers ALTER COLUMN password DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_students_google_subject
    ON students (google_subject) WHERE google_subject IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_teachers_teacher_id
    ON teachers (teacher_id) WHERE teacher_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_teachers_google_subject
    ON teachers (google_subject) WHERE google_subject IS NOT NULL;
