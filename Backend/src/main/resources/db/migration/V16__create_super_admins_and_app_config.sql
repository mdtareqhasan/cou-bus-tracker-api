-- ============================================================================
-- V16: Super Admin & App Config tables
-- ============================================================================
-- Creates:
--   1. super_admins - privileged users who can manage system configuration
--   2. app_config   - key/value runtime configuration (base URL, version,
--                     maintenance mode, etc.) read by the Flutter app via
--                     /api/config and edited by the super admin panel.
-- ============================================================================

CREATE TABLE IF NOT EXISTS super_admins (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_super_admins_email ON super_admins(email);

CREATE TABLE IF NOT EXISTS app_config (
    id            BIGSERIAL    PRIMARY KEY,
    config_key    VARCHAR(100) NOT NULL UNIQUE,
    config_value  TEXT         NOT NULL,
    description   VARCHAR(500),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_app_config_key ON app_config(config_key);

-- ---------------------------------------------------------------------------
-- Default super admin is seeded by SuperAdminDataInitializer (ApplicationRunner)
-- so the BCrypt hash is generated at startup using the configured
-- PasswordEncoder bean (guaranteed to verify).
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- Seed default app config values
-- ---------------------------------------------------------------------------
INSERT INTO app_config (config_key, config_value, description) VALUES
    ('api_base_url',            'http://localhost:8080/api',                                                        'Backend API base URL consumed by the Flutter app'),
    ('admin_panel_url',         'http://localhost:5173',                                                            'Main admin panel URL'),
    ('super_admin_panel_url',   'http://localhost:5174',                                                            'Super admin panel URL'),
    ('latest_app_version',      '1.0.0',                                                                            'Latest published Flutter app version'),
    ('minimum_app_version',     '1.0.0',                                                                            'Minimum Flutter app version that can run'),
    ('force_update',            'false',                                                                            'Force users to update Flutter app (true/false)'),
    ('update_available_message','নতুন ভার্সন পাওয়া গেছে! আপডেট করুন।',                                            'Bengali update prompt shown to users'),
    ('play_store_url',          'https://play.google.com/store/apps/details?id=com.cou.bustracker',                'Play Store URL opened when user taps Update'),
    ('maintenance_mode',        'false',                                                                            'If true, Flutter app displays maintenance screen'),
    ('maintenance_message',     'অ্যাপটি রক্ষণাবেক্ষণে আছে। শীঘ্রই ফিরে আসছি।',                                  'Bengali maintenance message')
ON CONFLICT (config_key) DO NOTHING;
