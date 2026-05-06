ALTER TABLE entity_events.config
    ADD COLUMN target_key text,
ADD COLUMN target_key_type text,
ADD COLUMN target_value text;