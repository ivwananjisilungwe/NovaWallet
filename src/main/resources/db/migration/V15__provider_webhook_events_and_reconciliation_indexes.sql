-- V15: Batch 3 (B3.3) disbursement webhook persistence + B3.2 reconciliation indexes.
--
-- 1. Durable webhook event store: idempotent intake (provider,event_id) and a
--    quarantine path for mismatched/unmatched callbacks. Duplicate deliveries
--    of the same event never produce duplicate financial effects.
CREATE TABLE provider_webhook_events (
    id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    reference_id VARCHAR(255),
    event_type VARCHAR(50),
    payload TEXT,
    signature_ok BOOLEAN NOT NULL DEFAULT FALSE,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    PRIMARY KEY (id),
    CONSTRAINT uq_provider_webhook_event UNIQUE (provider, event_id)
);

CREATE INDEX idx_provider_webhook_events_reference
    ON provider_webhook_events (reference_id);

-- 2. Guard the reconciliation attempt counter at the database level
--    (mirrors the application-level @Transactional claim logic).
ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_attempt_count_non_negative
    CHECK (attempt_count >= 0);

-- 3. Indexes backing the reconciliation scheduler's polling queries.
--    Partial-index WHERE clauses (PostgreSQL-specific) are intentionally
--    omitted so the same migration applies cleanly under H2 in tests; the
--    selectivity gained by the WHERE is reproduced by the leading columns.
--    NOTE: the single-column (provider_reference) index is intentionally NOT
--    recreated here — V14 already creates the identical
--    idx_transactions_provider_reference, and a second copy would only add
--    write amplification with no selectivity gain.
CREATE INDEX idx_transactions_reconciliation_due
    ON transactions (next_status_check_at, status, provider_reference);

-- NOTE: an external_status CHECK is intentionally NOT added. Provider status
-- strings beyond the canonical set (e.g. 'Processing') flow through the
-- uncertain/PENDING_RECONCILIATION branch and must not be rejected at the DB.
