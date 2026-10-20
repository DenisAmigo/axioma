-- ============================================================
-- Axioma — начальная схема БД (ядро приложения)
-- Flyway migration V1
-- ============================================================

-- ------------------------------------------------------------
-- users: клиенты, админы, саппорт
-- ------------------------------------------------------------
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(255),
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'DELETED'))
);

CREATE INDEX idx_users_status ON users(status);

COMMENT ON TABLE users IS 'Пользователи системы: клиенты, администраторы, поддержка';
COMMENT ON COLUMN users.password_hash IS 'BCrypt-хеш пароля';
COMMENT ON COLUMN users.status IS 'ACTIVE | BLOCKED | DELETED';

-- ------------------------------------------------------------
-- roles: справочник ролей
-- ------------------------------------------------------------
CREATE TABLE roles (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(50)     NOT NULL UNIQUE,
    description     VARCHAR(255)
);

COMMENT ON TABLE roles IS 'Роли: CLIENT, ADMIN, SUPPORT';

-- ------------------------------------------------------------
-- user_roles: many-to-many
-- ------------------------------------------------------------
CREATE TABLE user_roles (
    user_id         BIGINT          NOT NULL,
    role_id         BIGINT          NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- ------------------------------------------------------------
-- accounts: счета клиентов
-- ------------------------------------------------------------
CREATE TABLE accounts (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    account_number  VARCHAR(20)     NOT NULL UNIQUE,
    currency        VARCHAR(3)      NOT NULL,
    balance         NUMERIC(19,2)   NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT chk_accounts_currency CHECK (currency IN ('RUB', 'USD', 'EUR')),
    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_user ON accounts(user_id);
CREATE INDEX idx_accounts_status ON accounts(status);

COMMENT ON TABLE accounts IS 'Счета клиентов. balance — кеш от ledger_entries, источник истины — леджер';
COMMENT ON COLUMN accounts.balance IS 'Текущий баланс. Проверка: должен совпадать с суммой проводок';
COMMENT ON COLUMN accounts.version IS 'Для optimistic locking (@Version в JPA)';

-- ------------------------------------------------------------
-- transfers: переводы (бизнес-операции)
-- ------------------------------------------------------------
CREATE TABLE transfers (
    id                  BIGSERIAL       PRIMARY KEY,
    from_account_id     BIGINT          NOT NULL,
    to_account_id       BIGINT          NOT NULL,
    amount              NUMERIC(19,2)   NOT NULL,
    currency            VARCHAR(3)      NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    description         VARCHAR(500),
    idempotency_key     VARCHAR(255)    UNIQUE,
    failure_reason      VARCHAR(500),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ,
    CONSTRAINT fk_transfers_from FOREIGN KEY (from_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transfers_to FOREIGN KEY (to_account_id) REFERENCES accounts(id),
    CONSTRAINT chk_transfers_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_transfers_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transfers_accounts_differ CHECK (from_account_id <> to_account_id)
);

CREATE INDEX idx_transfers_from_created ON transfers(from_account_id, created_at DESC);
CREATE INDEX idx_transfers_to_created ON transfers(to_account_id, created_at DESC);
CREATE INDEX idx_transfers_status ON transfers(status);
CREATE INDEX idx_transfers_created ON transfers(created_at DESC);

COMMENT ON TABLE transfers IS 'Переводы между счетами. Одна запись = одна бизнес-операция';
COMMENT ON COLUMN transfers.idempotency_key IS 'Ключ идемпотентности от клиента (Idempotency-Key header)';
COMMENT ON COLUMN transfers.failure_reason IS 'Причина отказа, если status = FAILED';

-- ------------------------------------------------------------
-- ledger_entries: проводки (движения денег, double-entry)
-- ------------------------------------------------------------
CREATE TABLE ledger_entries (
    id              BIGSERIAL       PRIMARY KEY,
    transfer_id     BIGINT          NOT NULL,
    account_id      BIGINT          NOT NULL,
    entry_type      VARCHAR(10)     NOT NULL,
    amount          NUMERIC(19,2)   NOT NULL,
    balance_after   NUMERIC(19,2)   NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT fk_ledger_transfer FOREIGN KEY (transfer_id) REFERENCES transfers(id),
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT chk_ledger_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ledger_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_ledger_account_created ON ledger_entries(account_id, created_at DESC);
CREATE INDEX idx_ledger_transfer ON ledger_entries(transfer_id);
CREATE INDEX idx_ledger_created ON ledger_entries(created_at DESC);

COMMENT ON TABLE ledger_entries IS 'Проводки. Двойная запись: на каждый transfer — минимум 2 записи (DEBIT + CREDIT)';
COMMENT ON COLUMN ledger_entries.entry_type IS 'DEBIT — списание, CREDIT — зачисление';
COMMENT ON COLUMN ledger_entries.amount IS 'Всегда положительное. Направление задаётся entry_type';
COMMENT ON COLUMN ledger_entries.balance_after IS 'Баланс счёта после этой проводки (денормализация для отладки и отчётов)';

-- ------------------------------------------------------------
-- idempotency_keys: ключи идемпотентности
-- ------------------------------------------------------------
CREATE TABLE idempotency_keys (
    key                 VARCHAR(255)    PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    request_hash        VARCHAR(64)     NOT NULL,
    response_body       TEXT,
    response_status     INT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ     NOT NULL,
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);
CREATE INDEX idx_idempotency_user ON idempotency_keys(user_id);

COMMENT ON TABLE idempotency_keys IS 'Ключи идемпотентности. Защита от дублирующих запросов';
COMMENT ON COLUMN idempotency_keys.request_hash IS 'SHA-256 от тела запроса. Если ключ тот же, а хеш другой — 409 Conflict';
COMMENT ON COLUMN idempotency_keys.expires_at IS 'Когда ключ можно удалить (обычно +24ч или +48ч)';

-- ------------------------------------------------------------
-- Начальные данные: роли
-- ------------------------------------------------------------
INSERT INTO roles (name, description) VALUES
    ('CLIENT',  'Клиент — владелец счетов'),
    ('SUPPORT', 'Сотрудник поддержки — просмотр без изменения операций'),
    ('ADMIN',   'Администратор — полный доступ');