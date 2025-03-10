CREATE TABLE IF NOT EXISTS account_owner (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY UNIQUE,
    owner_id BIGINT NOT NULL,
    owner_type VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (owner_id, owner_type)
    );

CREATE TABLE IF NOT EXISTS account (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY UNIQUE,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    type VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMPTZ,
    version INT DEFAULT 1 NOT NULL,
    owner_id BIGINT NOT NULL,

    CONSTRAINT fk_owner_id FOREIGN KEY (owner_id)
    REFERENCES account_owner (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_account_owner_owner
    ON account_owner (owner_id, owner_type);