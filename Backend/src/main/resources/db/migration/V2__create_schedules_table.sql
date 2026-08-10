-- V2: Create schedules table
CREATE TABLE IF NOT EXISTS schedules (
    id              BIGSERIAL PRIMARY KEY,
    bus_id          BIGINT NOT NULL,
    departure_time  TIME NOT NULL,
    arrival_time    TIME,
    direction       VARCHAR(10) NOT NULL,
    start_point     VARCHAR(100),
    end_point       VARCHAR(100),
    days            VARCHAR(50) DEFAULT 'SAT-THU',
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedule_bus FOREIGN KEY (bus_id) REFERENCES buses(id) ON DELETE CASCADE
);
