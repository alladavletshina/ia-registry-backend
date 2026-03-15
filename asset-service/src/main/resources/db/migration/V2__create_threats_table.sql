CREATE TABLE IF NOT EXISTS threats (
    id VARCHAR(50) PRIMARY KEY,                -- идентификатор угрозы (например, "1")
    name VARCHAR(500) NOT NULL,                 -- наименование
    description TEXT,                           -- описание
    source TEXT,                                -- источник угрозы
    object_affected VARCHAR(255),                -- объект воздействия
    confidentiality BOOLEAN DEFAULT FALSE,       -- нарушение конфиденциальности
    integrity BOOLEAN DEFAULT FALSE,             -- нарушение целостности
    availability BOOLEAN DEFAULT FALSE,          -- нарушение доступности
    inclusion_date DATE,                         -- дата включения
    last_modified DATE,                          -- дата последнего изменения
    status VARCHAR(50),                          -- статус угрозы
    notes TEXT,                                  -- замечания
    synced_at DATE                               -- дата синхронизации
    );

-- Индекс для поиска по имени (если часто ищете)
CREATE INDEX idx_threats_name ON threats(name);

-- Индекс для сортировки по дате
CREATE INDEX idx_threats_last_modified ON threats(last_modified);