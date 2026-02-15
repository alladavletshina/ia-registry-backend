-- Создание таблицы assets
CREATE TABLE IF NOT EXISTS assets (
                                      id BIGSERIAL PRIMARY KEY,
                                      name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    owner_id VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    confidentiality VARCHAR(50) NOT NULL,
    integrity VARCHAR(50) NOT NULL,
    availability VARCHAR(50) NOT NULL,
    last_review VARCHAR(20),
    description TEXT,
    location VARCHAR(255),
    tags VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE
    );

-- Индексы для ускорения поиска
CREATE INDEX idx_assets_owner ON assets(owner);
CREATE INDEX idx_assets_status ON assets(status);
CREATE INDEX idx_assets_category ON assets(category);

-- Комментарии к таблице и колонкам (опционально)
COMMENT ON TABLE assets IS 'Информационные активы';
COMMENT ON COLUMN assets.name IS 'Наименование актива';
COMMENT ON COLUMN assets.category IS 'Категория (database, documentation, software и т.д.)';
COMMENT ON COLUMN assets.owner_id IS 'Владелец актива (ID пользователя)';
COMMENT ON COLUMN assets.status IS 'Статус: ACTIVE, NEEDS_REVIEW, ARCHIVED, DRAFT';
COMMENT ON COLUMN assets.confidentiality IS 'Уровень конфиденциальности: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN assets.integrity IS 'Уровень целостности: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN assets.availability IS 'Уровень доступности: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN assets.last_review IS 'Дата последней проверки (в формате YYYY-MM-DD)';
COMMENT ON COLUMN assets.description IS 'Описание актива';
COMMENT ON COLUMN assets.location IS 'Местоположение/хранение';
COMMENT ON COLUMN assets.tags IS 'Теги через запятую';
COMMENT ON COLUMN assets.created_at IS 'Дата создания записи';
COMMENT ON COLUMN assets.updated_at IS 'Дата последнего обновления';