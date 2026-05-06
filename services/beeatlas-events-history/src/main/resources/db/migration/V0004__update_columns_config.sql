ALTER TABLE entity_events.config
    ADD COLUMN IF NOT EXISTS get_source boolean DEFAULT true,
    ADD COLUMN IF NOT EXISTS created_date_alias text DEFAULT 'created_date',
    ADD COLUMN IF NOT EXISTS updated_date_alias text DEFAULT 'last_modified_date',
    ADD COLUMN IF NOT EXISTS deleted_date_alias text DEFAULT 'deleted_date',
    ADD COLUMN IF NOT EXISTS generate_update_event boolean DEFAULT true,
    ADD COLUMN IF NOT EXISTS generate_delete_event boolean DEFAULT true,
    ADD COLUMN IF NOT EXISTS name_alias text DEFAULT 'name';