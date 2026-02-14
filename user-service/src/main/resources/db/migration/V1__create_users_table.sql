CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    keycloak_id VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    position VARCHAR(100),
    department VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
    );

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);