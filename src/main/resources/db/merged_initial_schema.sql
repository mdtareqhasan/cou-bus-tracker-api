-- CoU Bus Tracker - consolidated bootstrap schema
--
-- Use this file only for a fresh PostgreSQL database. It represents the
-- final schema after V1..V26; it is not intended to be run against a database
-- that has already been migrated by Flyway.

BEGIN;

-- --------------------------------------------------------------------------
-- Core transport tables
-- --------------------------------------------------------------------------
CREATE TABLE buses (
    id BIGSERIAL PRIMARY KEY,
    bus_number VARCHAR(20) NOT NULL UNIQUE,
    bus_name VARCHAR(100),
    category VARCHAR(20) NOT NULL,
    route TEXT,
    driver_name VARCHAR(100),
    driver_phone VARCHAR(20),
    bus_image_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE schedules (
    id BIGSERIAL PRIMARY KEY,
    bus_id BIGINT NOT NULL REFERENCES buses(id) ON DELETE CASCADE,
    departure_time TIME NOT NULL,
    arrival_time TIME,
    direction VARCHAR(10) NOT NULL,
    start_point VARCHAR(100),
    end_point VARCHAR(100),
    days VARCHAR(50) NOT NULL DEFAULT 'SUN-THU',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tracker_links (
    id BIGSERIAL PRIMARY KEY,
    bus_id BIGINT NOT NULL UNIQUE REFERENCES buses(id) ON DELETE CASCADE,
    tracker_url TEXT NOT NULL,
    expires_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

CREATE TABLE notices (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    expiry_hours INTEGER NOT NULL DEFAULT 24,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    audience VARCHAR(20) NOT NULL DEFAULT 'ALL',
    CONSTRAINT chk_notices_audience CHECK (audience IN ('STUDENT', 'TEACHER', 'ALL'))
);

CREATE TABLE admins (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- --------------------------------------------------------------------------
-- Academic reference data
-- --------------------------------------------------------------------------
CREATE TABLE academic_units (
    id BIGSERIAL PRIMARY KEY,
    unit_type VARCHAR(20) NOT NULL,
    name_bn VARCHAR(200) NOT NULL,
    name_en VARCHAR(200) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_academic_units_type CHECK (unit_type IN ('DEPARTMENT', 'OFFICE')),
    CONSTRAINT uk_academic_units_type_name UNIQUE (unit_type, name_bn)
);

CREATE TABLE academic_sessions (
    id BIGSERIAL PRIMARY KEY,
    name_bn VARCHAR(20) NOT NULL UNIQUE,
    name_en VARCHAR(20) NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- --------------------------------------------------------------------------
-- User tables (final form)
-- --------------------------------------------------------------------------
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    password VARCHAR(255),
    roll_number VARCHAR(50),
    department_id BIGINT REFERENCES academic_units(id),
    session_id BIGINT REFERENCES academic_sessions(id),
    id_card_image_url TEXT,
    google_subject VARCHAR(255),
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE teachers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    password VARCHAR(255),
    designation VARCHAR(100),
    department_id BIGINT REFERENCES academic_units(id),
    teacher_id VARCHAR(50),
    id_card_image_url TEXT,
    google_subject VARCHAR(255),
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN students.roll_number IS 'University roll number from ID card (e.g., 12208055)';
COMMENT ON COLUMN students.session IS 'Academic session from ID card (e.g., 2021-22)';

CREATE UNIQUE INDEX uk_students_email ON students(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uk_teachers_email ON teachers(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uk_students_google_subject ON students(google_subject) WHERE google_subject IS NOT NULL;
CREATE UNIQUE INDEX uk_teachers_teacher_id ON teachers(teacher_id) WHERE teacher_id IS NOT NULL;
CREATE UNIQUE INDEX uk_teachers_google_subject ON teachers(google_subject) WHERE google_subject IS NOT NULL;

-- --------------------------------------------------------------------------
-- Verification, administration, and notifications
-- --------------------------------------------------------------------------
CREATE TABLE email_verification_otps (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    user_role VARCHAR(20) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    last_sent_at TIMESTAMP NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    pending_registration_json TEXT,
    pending_id_card_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_email_verification_otps_email_role UNIQUE (email, user_role)
);

CREATE TABLE super_admins (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE app_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    description VARCHAR(500),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(150)
);

CREATE TABLE user_fcm_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_type VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    token TEXT NOT NULL UNIQUE,
    platform VARCHAR(30),
    device_id VARCHAR(150),
    app_language VARCHAR(10),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_user_fcm_tokens_user_type CHECK (user_type IN ('STUDENT', 'TEACHER'))
);

CREATE UNIQUE INDEX uk_user_fcm_tokens_device
    ON user_fcm_tokens(user_type, user_id, device_id)
    WHERE device_id IS NOT NULL;

CREATE INDEX idx_students_department_id ON students(department_id);
CREATE INDEX idx_students_session_id ON students(session_id);
CREATE INDEX idx_teachers_department_id ON teachers(department_id);
CREATE INDEX idx_super_admins_email ON super_admins(email);
CREATE INDEX idx_app_config_key ON app_config(config_key);
CREATE INDEX idx_user_fcm_tokens_user ON user_fcm_tokens(user_type, user_id);
CREATE INDEX idx_user_fcm_tokens_active_user ON user_fcm_tokens(user_type, user_id, is_active);

-- Academic reference-data seed. These are the final V26 values.
INSERT INTO academic_units (unit_type, name_bn, name_en, sort_order) VALUES
('DEPARTMENT','কম্পিউটার সায়েন্স অ্যান্ড ইঞ্জিনিয়ারিং (CSE)','Computer Science and Engineering (CSE)',1),
('DEPARTMENT','গণিত','Mathematics',2),('DEPARTMENT','পদার্থবিজ্ঞান','Physics',3),
('DEPARTMENT','পরিসংখ্যান','Statistics',4),('DEPARTMENT','রসায়ন','Chemistry',5),
('DEPARTMENT','ফার্মেসি','Pharmacy',6),('DEPARTMENT','ইংরেজি','English',7),
('DEPARTMENT','বাংলা','Bangla',8),('DEPARTMENT','অর্থনীতি','Economics',9),
('DEPARTMENT','লোক প্রশাসন','Public Administration',10),('DEPARTMENT','নৃবিজ্ঞান','Anthropology',11),
('DEPARTMENT','গণযোগাযোগ ও সাংবাদিকতা','Mass Communication and Journalism',12),
('DEPARTMENT','প্রত্নতত্ত্ব','Archaeology',13),('DEPARTMENT','ব্যবস্থাপনা শিক্ষা','Management Studies',14),
('DEPARTMENT','অ্যাকাউন্টিং অ্যান্ড ইনফরমেশন সিস্টেমস','Accounting and Information Systems',15),
('DEPARTMENT','মার্কেটিং','Marketing',16),('DEPARTMENT','ফাইন্যান্স অ্যান্ড ব্যাংকিং','Finance and Banking',17),
('DEPARTMENT','ইনফরমেশন অ্যান্ড কমিউনিকেশন টেকনোলজি (ICT)','Information and Communication Technology (ICT)',18),
('DEPARTMENT','আইন','Law',19),
('OFFICE','উপাচার্যের কার্যালয়','Office of the Vice-Chancellor',1),
('OFFICE','উপ-উপাচার্যের কার্যালয়','Office of the Pro-Vice-Chancellor',2),
('OFFICE','কোষাধ্যক্ষের কার্যালয়','Office of the Treasurer',3),
('OFFICE','রেজিস্ট্রারের কার্যালয়','Office of the Registrar',4),
('OFFICE','প্রক্টরের কার্যালয়','Office of the Proctor',5),
('OFFICE','গবেষণা ও সম্প্রসারণ পরিচালকের কার্যালয়','Office of the Director of Research & Extension',6),
('OFFICE','এস্টেট শাখা','Estate Section',7),('OFFICE','নিরাপত্তা শাখা','Security Section',8),
('OFFICE','কেন্দ্রীয় স্টোর','Central Store',9),
('OFFICE','অর্থ ও হিসাব পরিচালকের কার্যালয়','Office of the Director of Finance & Accounts',10),
('OFFICE','পরীক্ষা নিয়ন্ত্রকের কার্যালয়','Office of the Controller of Examinations',11),
('OFFICE','পরিকল্পনা ও উন্নয়ন পরিচালকের কার্যালয়','Office of the Director of Planning & Development',12),
('OFFICE','কেন্দ্রীয় গ্রন্থাগার','Central Library',13),('OFFICE','প্রকৌশল কার্যালয়','Engineering Office',14),
('OFFICE','মেডিকেল সেন্টার','Medical Center',15),('OFFICE','আইসিটি সেল','ICT Cell',16),
('OFFICE','শারীরিক শিক্ষা কার্যালয়','Physical Education Office',17),
('OFFICE','জনসংযোগ কার্যালয়','Public Relations Office',18),('OFFICE','কেন্দ্রীয় মসজিদ','Central Mosque',19),
('OFFICE','পরিবহন পুল','Transport Pool',20),
('OFFICE','শিক্ষার্থী কাউন্সেলিং ও গাইডেন্স','Student Counseling & Guidance',21),
('OFFICE','আন্তর্জাতিক সম্পর্ক','International Relations',22),
('OFFICE','ক্যারিয়ার ডেভেলপমেন্ট সেন্টার','Career Development Center',23);

INSERT INTO academic_sessions (name_bn, name_en, sort_order) VALUES
('২০১৬-২০১৭','2016-2017',1),('২০১৭-২০১৮','2017-2018',2),('২০১৮-২০১৯','2018-2019',3),
('২০১৯-২০২০','2019-2020',4),('২০২০-২০২১','2020-2021',5),('২০২১-২০২২','2021-2022',6),
('২০২২-২০২৩','2022-2023',7),('২০২৩-২০২৪','2023-2024',8),('২০২৪-২০২৫','2024-2025',9),
('২০২৫-২০২৬','2025-2026',10);

-- --------------------------------------------------------------------------
-- Initial transport data
-- --------------------------------------------------------------------------
INSERT INTO buses (bus_number, category, route) VALUES
('BUS 03', 'BLUE', 'Kandirpar → Campus via Policeline'),
('BUS 04', 'BLUE', 'Kandirpar → Campus via Policeline'),
('BUS 05', 'BLUE', 'Kandirpar → Campus via Policeline'),
('BUS 11', 'BLUE', 'Dhormopur → Campus'),
('BUS 25', 'BLUE', 'Campus → Policeline → Eidgah'),
('BUS 31', 'BLUE', 'Kandirpar → Campus'),
('BUS 32', 'BLUE', 'Kandirpar → Campus via Tomsombridge'),
('BUS 33', 'BLUE', 'Kandirpar → Campus via Tomsombridge'),
('BUS 06', 'TEACHER', 'Policeline → Campus via Tomsombridge'),
('BUS 08', 'TEACHER', 'Kandirpar → Campus'),
('BUS 14', 'STAFF', 'Kandirpar → Campus');

INSERT INTO schedules (bus_id, departure_time, arrival_time, direction, start_point, end_point) VALUES
((SELECT id FROM buses WHERE bus_number = 'BUS 03'), '07:00', '08:00', 'UP', 'Kandirpar', 'Campus'),
((SELECT id FROM buses WHERE bus_number = 'BUS 03'), '16:00', '17:00', 'DOWN', 'Campus', 'Kandirpar'),
((SELECT id FROM buses WHERE bus_number = 'BUS 04'), '07:15', '08:15', 'UP', 'Kandirpar', 'Campus'),
((SELECT id FROM buses WHERE bus_number = 'BUS 04'), '16:15', '17:15', 'DOWN', 'Campus', 'Kandirpar'),
((SELECT id FROM buses WHERE bus_number = 'BUS 11'), '07:30', '08:30', 'UP', 'Dhormopur', 'Campus'),
((SELECT id FROM buses WHERE bus_number = 'BUS 11'), '16:30', '17:30', 'DOWN', 'Campus', 'Dhormopur');

INSERT INTO notices (title, body, expiry_hours) VALUES
('Welcome to CoU Bus Tracker', 'This system provides real-time bus tracking for Comilla University.', 168);

-- Original V6 admin seed. Password hash corresponds to Admin@123.
INSERT INTO admins (email, password, name) VALUES
('admin@cou.ac.bd', '$2b$10$CqtkmaHceRdasMd7kjzfQ.kw3TI/lvuabJc5i4laQOOjqIdAHWYXe', 'COU Admin');

-- The application creates the default super admin using its configured
-- PasswordEncoder. No password is hard-coded here.
INSERT INTO app_config (config_key, config_value, description) VALUES
('api_base_url', 'http://localhost:8080/api', 'Backend API base URL consumed by the Flutter app'),
('admin_panel_url', 'http://localhost:5173', 'Main admin panel URL'),
('super_admin_panel_url', 'http://localhost:5174', 'Super admin panel URL'),
('latest_app_version', '1.0.0', 'Latest published Flutter app version'),
('minimum_app_version', '1.0.0', 'Minimum Flutter app version that can run'),
('force_update', 'false', 'Force users to update Flutter app'),
('update_available_message', 'নতুন ভার্সন পাওয়া গেছে! আপডেট করুন।', 'Bengali update prompt'),
('play_store_url', 'https://play.google.com/store/apps/details?id=com.cou.bustracker', 'Play Store URL'),
('maintenance_mode', 'false', 'Whether the app is in maintenance mode'),
('maintenance_message', 'অ্যাপটি রক্ষণাবেক্ষণে আছে। শীঘ্রই ফিরে আসছি।', 'Bengali maintenance message');

COMMIT;
