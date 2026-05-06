DO
$$
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM entity_events.config LIMIT 1)
    THEN
       INSERT INTO entity_events.config (
                                        id,
                                        schema_name,
                                        table_name,
                                        entity_type,
                                        last_scan_date,
                                        id_alias,
                                        get_name,
                                        notification_required,
                                        get_source,
                                        created_date_alias,
                                        updated_date_alias,
                                        deleted_date_alias,
                                        generate_update_event,
                                        generate_delete_event,
                                        name_alias,
                                        clarification_event,
                                        children_id_alias
                                    )
                            VALUES
                                (
                                    1,
                                    'capability',
                                    'business_capability',
                                    'BUSINESS_CAPABILITY',
                                    '2025-12-17 01:43:49.615729',
                                    'id',
                                    True,
                                    True,
                                    True,
                                    'created_date',
                                    'last_modified_date',
                                    'deleted_date',
                                    False,
                                    False,
                                    'name',
                                    NULL,
                                    NULL
                                ),
                                (
                                    2,
                                    'capability',
                                    'tech_capability',
                                    'TECH_CAPABILITY',
                                    '2025-12-17 01:43:49.615729',
                                    'id',
                                    True,
                                    True,
                                    True,
                                    'created_date',
                                    'last_modified_date',
                                    'deleted_date',
                                    False,
                                    False,
                                    'name',
                                    NULL,
                                    NULL
                                );
END IF;
END $$;