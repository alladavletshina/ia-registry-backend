-- Создание таблицы групп активов (если ещё нет)
CREATE TABLE IF NOT EXISTS asset_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
    );

-- Добавление связи группы в таблицу assets (если ещё нет)
ALTER TABLE assets ADD COLUMN IF NOT EXISTS group_id UUID REFERENCES asset_groups(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_assets_group_id ON assets(group_id);

-- Добавление полей стоимости, весов CIA, правового статуса
ALTER TABLE assets ADD COLUMN IF NOT EXISTS value DECIMAL(19,2);
ALTER TABLE assets ADD COLUMN IF NOT EXISTS weight_c INTEGER NOT NULL DEFAULT 1;
ALTER TABLE assets ADD COLUMN IF NOT EXISTS weight_i INTEGER NOT NULL DEFAULT 1;
ALTER TABLE assets ADD COLUMN IF NOT EXISTS weight_a INTEGER NOT NULL DEFAULT 1;
ALTER TABLE assets ADD COLUMN IF NOT EXISTS legal_status VARCHAR(50);

-- Создание таблицы asset_threats
CREATE TABLE IF NOT EXISTS asset_threats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    threat_id BIGINT NOT NULL REFERENCES threats(id),
    probability DECIMAL(3,2) NOT NULL DEFAULT 0,
    custom_c BOOLEAN,
    custom_i BOOLEAN,
    custom_a BOOLEAN,
    mitigation_effect DECIMAL(3,2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    assessment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
    );

-- Создание таблицы истории рисков
CREATE TABLE IF NOT EXISTS risk_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    calculated_risk DECIMAL(19,2) NOT NULL,
    calculation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    calculation_details TEXT
    );
CREATE INDEX IF NOT EXISTS idx_risk_asset_id ON risk_history(asset_id);