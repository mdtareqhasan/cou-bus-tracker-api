-- =====================================================================
-- V26: academic reference data + drop the phone/SMS vertical
--
-- Two jobs in one migration (V25 is the last applied version; this file
-- has never run, so it is safe to rewrite):
--
--   1. Reference tables. The registration dropdown options move out of
--      the Flutter app (lib/features/auth/department_options.dart and
--      session_options.dart) and into the database, so adding a
--      department is a DB row instead of an app release.
--
--      Departments and offices are ONE table (`academic_units`) with a
--      `unit_type` flag: a teacher works in either a teaching department
--      ('গণিত') or an administrative office ('রেজিস্ট্রারের কার্যালয়'),
--      and the app's teacherDepartmentOptions is literally
--      departmentOptions + officeOptions. Students filter to DEPARTMENT.
--
--   2. students/teachers now reference those tables by FK id instead of
--      storing a free-text department/session string. There are no
--      existing users, so the old varchar columns are dropped outright
--      and the whole phone/SMS vertical goes with them (see below).
--
-- LOCALIZATION: every row carries BOTH language forms (name_bn / name_en)
-- and the GET endpoints return both, so the client renders whichever
-- matches its current locale. The server deliberately does not pick a
-- language — the payload is tiny and Flutter already owns the locale.
--
-- CANONICAL STORED VALUE — no longer the FK source of truth, but kept
-- byte-identical so display and admin tooling never see a difference:
--   * academic_units.name_bn is what students/teachers.department used
--     to store (the Bengali name, write-once).
--   * academic_sessions.name_en is what students.session used to store
--     (e.g. '2020-2021'); its bn form is the same range in Bengali digits.
--
-- sort_order preserves the exact dropdown order the app shipped with.
-- is_active lets an option be retired without deleting the row.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) academic_units — departments AND offices, discriminated by unit_type
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS academic_units (
    id         BIGSERIAL    PRIMARY KEY,
    unit_type  VARCHAR(20)  NOT NULL,
    name_bn    VARCHAR(200) NOT NULL,
    name_en    VARCHAR(200) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_academic_units_type CHECK (unit_type IN ('DEPARTMENT', 'OFFICE')),
    -- Bengali name is unique within its type: two offices may not share a
    -- name, and an office may not shadow a department's name.
    CONSTRAINT uk_academic_units_type_name UNIQUE (unit_type, name_bn)
);

-- ---------------------------------------------------------------------
-- 2) academic_sessions — the academic session ranges
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS academic_sessions (
    id         BIGSERIAL    PRIMARY KEY,
    name_bn    VARCHAR(20)  NOT NULL UNIQUE,  -- '২০২০-২০২১'
    name_en    VARCHAR(20)  NOT NULL UNIQUE,  -- '2020-2021' (canonical)
    sort_order INTEGER      NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- 3) Seed — transcribed verbatim from the Flutter option files.
-- ---------------------------------------------------------------------

-- Departments (student picker shows these; teacher picker shows
-- departments + offices).
INSERT INTO academic_units (unit_type, name_bn, name_en, sort_order) VALUES
('DEPARTMENT', 'কম্পিউটার সায়েন্স অ্যান্ড ইঞ্জিনিয়ারিং (CSE)', 'Computer Science and Engineering (CSE)', 1),
('DEPARTMENT', 'গণিত',              'Mathematics',                2),
('DEPARTMENT', 'পদার্থবিজ্ঞান',      'Physics',                    3),
('DEPARTMENT', 'পরিসংখ্যান',         'Statistics',                 4),
('DEPARTMENT', 'রসায়ন',             'Chemistry',                  5),
('DEPARTMENT', 'ফার্মেসি',          'Pharmacy',                   6),
('DEPARTMENT', 'ইংরেজি',            'English',                    7),
('DEPARTMENT', 'বাংলা',             'Bangla',                     8),
('DEPARTMENT', 'অর্থনীতি',          'Economics',                  9),
('DEPARTMENT', 'লোক প্রশাসন',       'Public Administration',     10),
('DEPARTMENT', 'নৃবিজ্ঞান',          'Anthropology',              11),
('DEPARTMENT', 'গণযোগাযোগ ও সাংবাদিকতা', 'Mass Communication and Journalism', 12),
('DEPARTMENT', 'প্রত্নতত্ত্ব',        'Archaeology',               13),
('DEPARTMENT', 'ব্যবস্থাপনা শিক্ষা',  'Management Studies',        14),
('DEPARTMENT', 'অ্যাকাউন্টিং অ্যান্ড ইনফরমেশন সিস্টেমস', 'Accounting and Information Systems', 15),
('DEPARTMENT', 'মার্কেটিং',          'Marketing',                 16),
('DEPARTMENT', 'ফাইন্যান্স অ্যান্ড ব্যাংকিং', 'Finance and Banking', 17),
('DEPARTMENT', 'ইনফরমেশন অ্যান্ড কমিউনিকেশন টেকনোলজি (ICT)', 'Information and Communication Technology (ICT)', 18),
('DEPARTMENT', 'আইন',               'Law',                       19);

-- Offices (teacher-only units; appended after departments in the teacher
-- picker, matching teacherDepartmentOptions = [...departmentOptions,
-- ...officeOptions]).
INSERT INTO academic_units (unit_type, name_bn, name_en, sort_order) VALUES
('OFFICE', 'উপাচার্যের কার্যালয়',           'Office of the Vice-Chancellor',              1),
('OFFICE', 'উপ-উপাচার্যের কার্যালয়',        'Office of the Pro-Vice-Chancellor',          2),
('OFFICE', 'কোষাধ্যক্ষের কার্যালয়',         'Office of the Treasurer',                    3),
('OFFICE', 'রেজিস্ট্রারের কার্যালয়',        'Office of the Registrar',                    4),
('OFFICE', 'প্রক্টরের কার্যালয়',            'Office of the Proctor',                      5),
('OFFICE', 'গবেষণা ও সম্প্রসারণ পরিচালকের কার্যালয়', 'Office of the Director of Research & Extension', 6),
('OFFICE', 'এস্টেট শাখা',                   'Estate Section',                             7),
('OFFICE', 'নিরাপত্তা শাখা',                 'Security Section',                            8),
('OFFICE', 'কেন্দ্রীয় স্টোর',               'Central Store',                               9),
('OFFICE', 'অর্থ ও হিসাব পরিচালকের কার্যালয়', 'Office of the Director of Finance & Accounts', 10),
('OFFICE', 'পরীক্ষা নিয়ন্ত্রকের কার্যালয়',   'Office of the Controller of Examinations',   11),
('OFFICE', 'পরিকল্পনা ও উন্নয়ন পরিচালকের কার্যালয়', 'Office of the Director of Planning & Development', 12),
('OFFICE', 'কেন্দ্রীয় গ্রন্থাগার',           'Central Library',                            13),
('OFFICE', 'প্রকৌশল কার্যালয়',              'Engineering Office',                         14),
('OFFICE', 'মেডিকেল সেন্টার',               'Medical Center',                             15),
('OFFICE', 'আইসিটি সেল',                    'ICT Cell',                                   16),
('OFFICE', 'শারীরিক শিক্ষা কার্যালয়',        'Physical Education Office',                  17),
('OFFICE', 'জনসংযোগ কার্যালয়',              'Public Relations Office',                    18),
('OFFICE', 'কেন্দ্রীয় মসজিদ',               'Central Mosque',                             19),
('OFFICE', 'পরিবহন পুল',                    'Transport Pool',                             20),
('OFFICE', 'শিক্ষার্থী কাউন্সেলিং ও গাইডেন্স', 'Student Counseling & Guidance',            21),
('OFFICE', 'আন্তর্জাতিক সম্পর্ক',            'International Relations',                    22),
('OFFICE', 'ক্যারিয়ার ডেভেলপমেন্ট সেন্টার',  'Career Development Center',                 23);

-- Sessions. name_en is the canonical stored value ('2020-2021'); name_bn
-- is the same range rendered in Bengali digits for display.
INSERT INTO academic_sessions (name_bn, name_en, sort_order) VALUES
('২০১৬-২০১৭', '2016-2017',  1),
('২০১৭-২০১৮', '2017-2018',  2),
('২০১৮-২০১৯', '2018-2019',  3),
('২০১৯-২০২০', '2019-2020',  4),
('২০২০-২০২১', '2020-2021',  5),
('২০২১-২০২২', '2021-2022',  6),
('২০২২-২০২৩', '2022-2023',  7),
('২০২৩-২০২৪', '2023-2024',  8),
('২০২৪-২০২৫', '2024-2025',  9),
('২০২৫-২০২৬', '2025-2026', 10);

-- ---------------------------------------------------------------------
-- 4) Rewire students → academic reference tables.
-- No existing users, so the old varchar columns are dropped outright.
-- New FK columns are nullable: Google Sign-In creates the row before
-- academic details are filled in (V23 made these nullable for that flow).
-- ---------------------------------------------------------------------
ALTER TABLE students ADD COLUMN IF NOT EXISTS department_id BIGINT
    REFERENCES academic_units (id);
ALTER TABLE students ADD COLUMN IF NOT EXISTS session_id BIGINT
    REFERENCES academic_sessions (id);

ALTER TABLE students DROP COLUMN IF EXISTS department;
ALTER TABLE students DROP COLUMN IF EXISTS session;

-- ---------------------------------------------------------------------
-- 5) Rewire teachers → academic_units.
-- A teacher's department_id may point at a DEPARTMENT or an OFFICE row.
-- ---------------------------------------------------------------------
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS department_id BIGINT
    REFERENCES academic_units (id);

ALTER TABLE teachers DROP COLUMN IF EXISTS department;

-- ---------------------------------------------------------------------
-- 6) Drop the phone/SMS vertical. SMS registration is dead — the app is
-- Google-only now (e-mail registration is kept for future use, and it
-- never collected a phone). No account can hold a phone number, so the
-- columns, the OTP table, and is_phone_verified all go.
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS phone_verification_otps;

ALTER TABLE students DROP COLUMN IF EXISTS phone;
ALTER TABLE students DROP COLUMN IF EXISTS is_phone_verified;

ALTER TABLE teachers DROP COLUMN IF EXISTS phone;
ALTER TABLE teachers DROP COLUMN IF EXISTS is_phone_verified;

-- Indexes for the FK lookups the services now do.
CREATE INDEX IF NOT EXISTS idx_students_department_id ON students (department_id);
CREATE INDEX IF NOT EXISTS idx_students_session_id    ON students (session_id);
CREATE INDEX IF NOT EXISTS idx_teachers_department_id ON teachers (department_id);
