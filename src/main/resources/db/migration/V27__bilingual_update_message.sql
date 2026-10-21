-- Numbered V27 to sit above the last pre-merge migration (V26). Databases that
-- still carry the legacy flyway_schema_history would otherwise treat V1..V26 as
-- already applied — and V12 was the old `reseed_data_postgresql` script — so a
-- lower number would be silently skipped. A fresh database just runs V1 then
-- V27; a baselined database (baseline-version: 11) also applies V27.
--
-- Bilingual update prompt. The Flutter app picks the value that matches its
-- active locale and falls back to the other language (then to its own
-- translated default) when one is empty.
--
-- `update_available_message` is kept as the Bangla copy because clients that
-- predate locale-aware prompts only read that key. New clients read the
-- language-specific keys below.
INSERT INTO app_config (config_key, config_value, description) VALUES
    ('update_available_message_bn', 'নতুন ভার্সন পাওয়া গেছে! আপডেট করুন।', 'Bengali update prompt'),
    ('update_available_message_en', 'A new version of the app is available. Please update.', 'English update prompt')
ON CONFLICT (config_key) DO NOTHING;
