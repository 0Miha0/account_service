CREATE TABLE IF NOT EXISTS free_account_numbers
(
    account_type   VARCHAR(32) NOT NULL,
    account_number VARCHAR(20) NOT NULL UNIQUE PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS account_numbers_sequence
(
    account_type  VARCHAR(32) NOT NULL UNIQUE PRIMARY KEY,
    current_value BIGINT DEFAULT 0 NOT NULL
);

INSERT INTO account_numbers_sequence(account_type)
VALUES ('INDIVIDUAL'),
       ('SAVINGS'),
       ('DEBIT'),
       ('LEGAL');

CREATE INDEX IF NOT EXISTS idx_free_account_numbers_account_type
ON free_account_numbers(account_type);