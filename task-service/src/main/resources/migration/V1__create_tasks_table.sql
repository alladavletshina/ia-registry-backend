CREATE TABLE tasks (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       description TEXT,
                       priority VARCHAR(20) NOT NULL CHECK (priority IN ('HIGH','MEDIUM','LOW')),
                       status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','OVERDUE')),
                       type VARCHAR(20) NOT NULL CHECK (type IN ('UPDATE','REVIEW','REPORT','INVENTORY','BACKUP')),
                       due_date DATE,
                       completed_at DATE,
                       estimated_time VARCHAR(50),
                       asset_id BIGINT,
                       asset_name VARCHAR(255),
                       assigned_to UUID,
                       assigned_by UUID NOT NULL,
                       created_at DATE NOT NULL,
                       updated_at DATE NOT NULL
);

CREATE TABLE task_tags (
                           task_id BIGINT NOT NULL,
                           tag VARCHAR(50) NOT NULL,
                           CONSTRAINT fk_task_tags_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE INDEX idx_tasks_assigned_to ON tasks(assigned_to);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_asset_id ON tasks(asset_id);
CREATE INDEX idx_task_tags_tag ON task_tags(tag);