CREATE TABLE IF NOT EXISTS balance(
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY UNIQUE,
    account_id BIGINT UNIQUE NOT NULL,
    authorization_balance DECIMAL(18, 2) DEFAULT 0 NOT NULL,
    actual_balance DECIMAL(18, 2) DEFAULT 0 NOT NULL,
    created_at timestamptz DEFAULT current_timestamp NOT NULL,
    updated_at timestamptz DEFAULT current_timestamp NOT NULL,
    version BIGINT DEFAULT 1 NOT NULL,

    CONSTRAINT fk_account_number
        FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE
);