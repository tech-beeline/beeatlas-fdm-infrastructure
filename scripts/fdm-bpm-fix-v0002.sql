-- Разовая правка для стендов, где уже была применена старая V0002 (неверный checksum / старый target_call).
-- Запускается ДО fdm-bpm. При первом подъёме flyway_schema_history ещё нет — скрипт ничего не делает.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'processes'
          AND table_name = 'flyway_schema_history'
    ) THEN
        RAISE NOTICE 'fdm-bpm-fix-v0002: первый подъём (нет flyway_schema_history) — пропуск';
        RETURN;
END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM processes.flyway_schema_history
        WHERE version = '0002'
    ) THEN
        RAISE NOTICE 'fdm-bpm-fix-v0002: нет записи V0002 в history — пропуск';
        RETURN;
END IF;

    -- Checksum актуального V0002 в образе fdm-bpm
UPDATE processes.flyway_schema_history
SET checksum = 331383802
WHERE version = '0002'
  AND checksum IS DISTINCT FROM 331383802;

IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'processes'
          AND table_name = 'application_type_enum'
    ) THEN
UPDATE processes.application_type_enum
SET target_call = 'http://capability-backend:8080/api/v1/business-capability/public/{id}'
WHERE alias IN ('create_business_capability', 'update_business_capability')
  AND target_call IS DISTINCT FROM
    'http://capability-backend:8080/api/v1/business-capability/public/{id}';
END IF;

    RAISE NOTICE 'fdm-bpm-fix-v0002: checksum V0002 и target_call обновлены для существующего стенда';
END $$;
