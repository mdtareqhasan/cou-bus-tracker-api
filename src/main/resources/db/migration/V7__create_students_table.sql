-- V7: Create students table
CREATE TABLE IF NOT EXISTS students (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    student_id  VARCHAR(50) NOT NULL,
    department  VARCHAR(100) NOT NULL,
    varsity_batch VARCHAR(20) NOT NULL,
    id_card_image_url TEXT,
    is_edu_mail BOOLEAN DEFAULT FALSE,
    is_verified BOOLEAN DEFAULT FALSE,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
