-- ============================================================
-- Flyway migration V3
-- ============================================================
ALTER TABLE ledger_entries ALTER COLUMN transfer_id DROP NOT NULL;