-- Добавление столбца created_date в таблицу packages, заполнение текущим временем

ALTER TABLE pack_loader.packages
ADD COLUMN created_date TIMESTAMP WITHOUT TIME ZONE;

UPDATE pack_loader.packages
SET created_date = NOW();

ALTER TABLE pack_loader.packages
ALTER COLUMN created_date SET NOT NULL;