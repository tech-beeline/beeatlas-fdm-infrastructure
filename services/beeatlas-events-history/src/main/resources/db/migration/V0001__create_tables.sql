CREATE TABLE entity_events.config (
                                      id integer PRIMARY KEY,
                                      schema_name TEXT NOT NULL,
                                      table_name TEXT NOT NULL,
                                      entity_type TEXT NOT NULL,
                                      last_scan_date timestamp without time zone NULL
);