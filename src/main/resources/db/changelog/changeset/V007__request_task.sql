CREATE TABLE IF NOT EXISTS request_task
(
    id               BIGSERIAL PRIMARY KEY,
    request_id       UUID        NOT NULL,
    handler          VARCHAR(64) NOT NULL,
    status           VARCHAR(32) NOT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    rollback_context VARCHAR(255),
    version          BIGINT    DEFAULT 1,

    CONSTRAINT fk_request FOREIGN KEY (request_id)
        REFERENCES request (idempotent_token) ON DELETE CASCADE
);