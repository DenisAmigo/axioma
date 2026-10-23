-- ============================================================
-- Axioma — outbox-паттерн
-- Flyway migration V5
-- ============================================================

-- ------------------------------------------------------------
-- outbox_events: события kafka
-- ------------------------------------------------------------
CREATE TABLE outbox_events (
    id                  BIGSERIAL       PRIMARY KEY,
    aggregate_type      VARCHAR(50)     NOT NULL,
    aggregate_id        BIGINT          NOT NULL,
    event_type          VARCHAR(50)     NOT NULL,
    payload             TEXT            NOT NULL, -- JSON
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, FAILED
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    processed_at        TIMESTAMPTZ,
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX idx_outbox_status ON outbox_events(status);