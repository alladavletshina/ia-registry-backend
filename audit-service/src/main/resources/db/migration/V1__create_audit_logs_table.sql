CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id UUID,
    username VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    details TEXT,
    ip VARCHAR(45),
    severity VARCHAR(10) NOT NULL,
    service_name VARCHAR(50),
    object_id VARCHAR(255),
    object_type VARCHAR(50)
    );

CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_severity ON audit_logs(severity);