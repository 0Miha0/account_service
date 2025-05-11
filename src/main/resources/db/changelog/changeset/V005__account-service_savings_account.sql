CREATE TABLE IF NOT EXISTS savings_account (
    id                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id          BIGINT NOT NULL UNIQUE,
    current_tariff_id   BIGINT NOT NULL,
    last_interest_date  TIMESTAMPTZ,
    version             BIGINT DEFAULT 1 NOT NULL,
    updated_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT account_id_fk FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tariff (
    id              BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name            VARCHAR(128) NOT NULL UNIQUE,
    current_rate    DECIMAL(4,2) NOT NULL,
    interest_period VARCHAR(16) NOT NULL,
    version         BIGINT DEFAULT 1 NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS tariff_rate_changelog (
    id          BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    tariff_id   BIGINT NOT NULL,
    rate        DECIMAL(4,2) NOT NULL,
    change_date TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT tariff_id_fk FOREIGN KEY (tariff_id) REFERENCES tariff (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS savings_account_tariff_changelog (
    id                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    savings_account_id  BIGINT NOT NULL,
    tariff_id           BIGINT NOT NULL,
    change_date         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT savings_account_id_fk FOREIGN KEY (savings_account_id) REFERENCES savings_account (id) ON DELETE CASCADE,
    CONSTRAINT tariff_id_fk FOREIGN KEY (tariff_id) REFERENCES tariff (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tariff_rate_changelog_tariff_date ON tariff_rate_changelog (tariff_id, change_date DESC);
CREATE INDEX IF NOT EXISTS idx_savings_account_tariff_change ON savings_account_tariff_changelog (savings_account_id, tariff_id, change_date DESC);
