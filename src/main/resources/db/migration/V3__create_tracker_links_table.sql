-- V3: Create tracker_links table
CREATE TABLE IF NOT EXISTS tracker_links (
    id          BIGSERIAL PRIMARY KEY,
    bus_id      BIGINT NOT NULL UNIQUE,
    tracker_url TEXT NOT NULL,
    expires_at  TIMESTAMP NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by  VARCHAR(100),
    CONSTRAINT fk_tracker_bus FOREIGN KEY (bus_id) REFERENCES buses(id) ON DELETE CASCADE
);
