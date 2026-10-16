-- V12: Seed data + fix missing sequences (DBeaver didn't create them)
-- Explicitly provides IDs since DBeaver created id as BIGINT NOT NULL without SERIAL.

-- ============================================================
-- Create sequences for tables where DBeaver missed them
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'buses_id_seq') THEN
        CREATE SEQUENCE IF NOT EXISTS buses_id_seq OWNED BY buses.id;
        ALTER TABLE buses ALTER COLUMN id SET DEFAULT nextval('buses_id_seq');
    END IF;
    PERFORM setval('buses_id_seq', COALESCE((SELECT MAX(id) FROM buses), 1));

    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'schedules_id_seq') THEN
        CREATE SEQUENCE IF NOT EXISTS schedules_id_seq OWNED BY schedules.id;
        ALTER TABLE schedules ALTER COLUMN id SET DEFAULT nextval('schedules_id_seq');
    END IF;
    PERFORM setval('schedules_id_seq', COALESCE((SELECT MAX(id) FROM schedules), 1));

    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'notices_id_seq') THEN
        CREATE SEQUENCE IF NOT EXISTS notices_id_seq OWNED BY notices.id;
        ALTER TABLE notices ALTER COLUMN id SET DEFAULT nextval('notices_id_seq');
    END IF;
    PERFORM setval('notices_id_seq', COALESCE((SELECT MAX(id) FROM notices), 1));

    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'tracker_links_id_seq') THEN
        CREATE SEQUENCE IF NOT EXISTS tracker_links_id_seq OWNED BY tracker_links.id;
        ALTER TABLE tracker_links ALTER COLUMN id SET DEFAULT nextval('tracker_links_id_seq');
    END IF;
    PERFORM setval('tracker_links_id_seq', COALESCE((SELECT MAX(id) FROM tracker_links), 1));

    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'admins_id_seq') THEN
        CREATE SEQUENCE IF NOT EXISTS admins_id_seq OWNED BY admins.id;
        ALTER TABLE admins ALTER COLUMN id SET DEFAULT nextval('admins_id_seq');
    END IF;
    PERFORM setval('admins_id_seq', COALESCE((SELECT MAX(id) FROM admins), 1));

    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'students_id_seq') THEN
        CREATE SEQUENCE IF NOT EXISTS students_id_seq OWNED BY students.id;
        ALTER TABLE students ALTER COLUMN id SET DEFAULT nextval('students_id_seq');
    END IF;
    PERFORM setval('students_id_seq', COALESCE((SELECT MAX(id) FROM students), 1));

    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'teachers_id_seq') THEN
        CREATE SEQUENCE IF NOT EXISTS teachers_id_seq OWNED BY teachers.id;
        ALTER TABLE teachers ALTER COLUMN id SET DEFAULT nextval('teachers_id_seq');
    END IF;
    PERFORM setval('teachers_id_seq', COALESCE((SELECT MAX(id) FROM teachers), 1));
END $$;

-- ============================================================
-- BUSES
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM buses LIMIT 1) THEN
        INSERT INTO buses (id, bus_number, bus_name, category, route, is_active) VALUES
        (1,  'BUS 03', NULL, 'BLUE', 'Kandirpar → Campus via Policeline', TRUE),
        (2,  'BUS 04', NULL, 'BLUE', 'Kandirpar → Campus via Policeline', TRUE),
        (3,  'BUS 05', NULL, 'BLUE', 'Kandirpar → Campus via Policeline', TRUE),
        (4,  'BUS 11', NULL, 'BLUE', 'Dhormopur → Campus', TRUE),
        (5,  'BUS 25', NULL, 'BLUE', 'Campus → Policeline → Eidgah', TRUE),
        (6,  'BUS 31', NULL, 'BLUE', 'Kandirpar → Campus', TRUE),
        (7,  'BUS 32', NULL, 'BLUE', 'Kandirpar → Campus via Tomsombridge', TRUE),
        (8,  'BUS 33', NULL, 'BLUE', 'Kandirpar → Campus via Tomsombridge', TRUE),
        (9,  'BUS 06', NULL, 'TEACHER', 'Policeline → Campus via Tomsombridge', TRUE),
        (10, 'BUS 08', NULL, 'TEACHER', 'Kandirpar → Campus', TRUE),
        (11, 'BUS 14', NULL, 'STAFF', 'Kandirpar → Campus', TRUE);
        PERFORM setval('buses_id_seq', 11);
    END IF;
END $$;

-- ============================================================
-- SCHEDULES
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM schedules LIMIT 1) THEN
        INSERT INTO schedules (id, bus_id, departure_time, arrival_time, direction, start_point, end_point, days, is_active) VALUES
        (1, (SELECT id FROM buses WHERE bus_number = 'BUS 03'), '07:00', '08:00', 'UP', 'Kandirpar', 'Campus', 'SUN-THU', TRUE),
        (2, (SELECT id FROM buses WHERE bus_number = 'BUS 03'), '16:00', '17:00', 'DOWN', 'Campus', 'Kandirpar', 'SUN-THU', TRUE),
        (3, (SELECT id FROM buses WHERE bus_number = 'BUS 04'), '07:15', '08:15', 'UP', 'Kandirpar', 'Campus', 'SUN-THU', TRUE),
        (4, (SELECT id FROM buses WHERE bus_number = 'BUS 04'), '16:15', '17:15', 'DOWN', 'Campus', 'Kandirpar', 'SUN-THU', TRUE),
        (5, (SELECT id FROM buses WHERE bus_number = 'BUS 11'), '07:30', '08:30', 'UP', 'Dhormopur', 'Campus', 'SUN-THU', TRUE),
        (6, (SELECT id FROM buses WHERE bus_number = 'BUS 11'), '16:30', '17:30', 'DOWN', 'Campus', 'Dhormopur', 'SUN-THU', TRUE);
        PERFORM setval('schedules_id_seq', 6);
    END IF;
END $$;

-- ============================================================
-- NOTICES
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM notices LIMIT 1) THEN
        INSERT INTO notices (id, title, body, expiry_hours, is_active, expires_at) VALUES
        (1, 'Welcome to CoU Bus Tracker', 'This system provides real-time bus tracking for Comilla University.', 168, TRUE, NULL);
        PERFORM setval('notices_id_seq', 1);
    END IF;
END $$;
