-- V1: Create buses table
CREATE TABLE IF NOT EXISTS buses (
    id          BIGSERIAL PRIMARY KEY,
    bus_number  VARCHAR(20) NOT NULL UNIQUE,
    bus_name    VARCHAR(100),
    category    VARCHAR(20) NOT NULL,
    route       TEXT,
    driver_name VARCHAR(100),
    driver_phone VARCHAR(20),
    bus_image_url TEXT,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
