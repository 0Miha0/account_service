CREATE TABLE IF NOT EXISTS request
(
    idempotent_token UUID PRIMARY KEY,
    request_type     VARCHAR(64) NOT NULL,
    request_status   VARCHAR(64) NOT NULL,
    context          VARCHAR(128),
    scheduled_at     TIMESTAMP
);