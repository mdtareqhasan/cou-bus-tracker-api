-- The app's chosen language ('bn' / 'en'), sent by the client when it
-- registers its FCM token. Push notifications are rendered server-side in
-- this language, so each device gets its push text in the language the rider
-- actually reads.
--
-- Nullable: tokens registered by older app versions have no value; the
-- notification dispatcher falls back to English for those.
ALTER TABLE user_fcm_tokens
    ADD COLUMN app_language VARCHAR(10);
