-- V10: Add bus_name column (optional display name for a bus)
ALTER TABLE buses ADD COLUMN IF NOT EXISTS bus_name VARCHAR(100) NULL;
