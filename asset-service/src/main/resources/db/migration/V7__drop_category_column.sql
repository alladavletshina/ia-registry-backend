-- Удаляем колонку category из таблицы assets
ALTER TABLE assets DROP COLUMN IF EXISTS category;