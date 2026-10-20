-- ============================================================
-- Flyway migration V2
-- ============================================================

-- ------------------------------------------------------------
-- deposit_transactions: зачисления наличных
-- ------------------------------------------------------------
CREATE TABLE deposit_transactions
(
    id                 BIGSERIAL PRIMARY KEY,
    account_id         BIGINT         NOT NULL,
    amount             NUMERIC(19, 2) NOT NULL,
    currency           VARCHAR(3)     NOT NULL,
    status             VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    atm_transaction_id VARCHAR(255),
    failure_reason     VARCHAR(500),
    idempotency_key    VARCHAR(255) UNIQUE,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    completed_at       TIMESTAMPTZ,
    CONSTRAINT fk_deposit_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT chk_deposit_status CHECK (status IN ('PENDING', 'CONFIRMED', 'FAILED', 'TIMEOUT')),
    CONSTRAINT chk_deposit_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_deposit_account ON deposit_transactions (account_id);
CREATE INDEX idx_deposit_status ON deposit_transactions (status);