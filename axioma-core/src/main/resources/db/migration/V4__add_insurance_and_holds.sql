-- ============================================================
-- Axioma — схема БД покупки ОСАГО (страховки и расчёты)
-- Flyway migration V4
-- ============================================================

-- ------------------------------------------------------------
-- accounts: поле для холдов в счета
-- ------------------------------------------------------------
ALTER TABLE accounts ADD COLUMN held_amount NUMERIC(19,2) NOT NULL DEFAULT 0.00;
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_held_amount_non_negative CHECK (held_amount >= 0);
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_held_less_than_balance CHECK (held_amount <= balance);

-- ------------------------------------------------------------
-- quotes: таблица расчетов
-- ------------------------------------------------------------
CREATE TABLE insurance_quotes
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT         NOT NULL,
    company_code     VARCHAR(50)    NOT NULL,
    amount           NUMERIC(19, 2) NOT NULL,
    coefficient_info VARCHAR(255),            -- Например: "Регион: Москва, Стаж: 10 лет"
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_quote_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- ------------------------------------------------------------
-- policies: таблица полисов
-- ------------------------------------------------------------
CREATE TABLE insurance_policies
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT         NOT NULL,
    account_id    BIGINT         NOT NULL,
    company_code  VARCHAR(50)    NOT NULL,
    amount        NUMERIC(19, 2) NOT NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'PENDING', -- PENDING, ISSUED, CANCELLED, EXPIRED
    policy_number VARCHAR(255),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ    NOT NULL,                   -- Время жизни холда (20 минут)
    completed_at  TIMESTAMPTZ,
    CONSTRAINT fk_policy_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_policy_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT chk_policy_status CHECK (status IN ('PENDING', 'ISSUED', 'CANCELLED', 'EXPIRED'))
);

CREATE INDEX idx_policy_status_expires ON insurance_policies (status, expires_at);