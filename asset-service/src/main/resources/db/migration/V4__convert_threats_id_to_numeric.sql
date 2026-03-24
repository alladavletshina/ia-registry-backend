-- Добавляем временную колонку с числовым типом
ALTER TABLE threats ADD COLUMN id_new BIGINT;

-- Копируем данные, преобразуя строковые id в числа
UPDATE threats SET id_new = CAST(id AS BIGINT);

-- Удаляем ограничения первичного ключа и индексы
ALTER TABLE threats DROP CONSTRAINT threats_pkey;
DROP INDEX idx_threats_name;
DROP INDEX idx_threats_last_modified;

-- Удаляем старую колонку id
ALTER TABLE threats DROP COLUMN id;

-- Переименовываем новую колонку
ALTER TABLE threats RENAME COLUMN id_new TO id;

-- Восстанавливаем первичный ключ
ALTER TABLE threats ADD PRIMARY KEY (id);

-- Восстанавливаем индексы
CREATE INDEX idx_threats_name ON threats(name);
CREATE INDEX idx_threats_last_modified ON threats(last_modified);