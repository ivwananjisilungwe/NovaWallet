-- V14: Add dedicated payout data columns to transactions (Batch 3 B3.1).
-- Additive only: all columns nullable except attempt_count (default 0).
-- Non-payout rows (deposits, transfers, fees) remain unaffected (NULL).

ALTER TABLE transactions ADD COLUMN provider_reference VARCHAR(255);
ALTER TABLE transactions ADD COLUMN external_status VARCHAR(50);
ALTER TABLE transactions ADD COLUMN payout_account VARCHAR(255);
ALTER TABLE transactions ADD COLUMN payout_channel VARCHAR(50);
ALTER TABLE transactions ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE transactions ADD COLUMN next_status_check_at TIMESTAMP;
ALTER TABLE transactions ADD COLUMN idempotency_key VARCHAR(255);

-- Indexes for reconciliation lookups and dedup.
CREATE INDEX idx_transactions_provider_reference ON transactions(provider_reference);
CREATE INDEX idx_transactions_external_status ON transactions(external_status);
CREATE INDEX idx_transactions_payout_channel ON transactions(payout_channel);
CREATE INDEX idx_transactions_next_status_check_at ON transactions(next_status_check_at);
CREATE UNIQUE INDEX idx_transactions_idempotency_key ON transactions(idempotency_key);

-- Backfill is intentionally omitted here: historical rows keep description markers.
-- PayoutService populates the dedicated columns for all new payouts going forward.
-- A separate one-time backfill job can be run if historical extraction is needed.