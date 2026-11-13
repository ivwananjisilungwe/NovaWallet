package com.novawallet.novawallet_api.payment.entity;

/**
 * Lifecycle state of a persisted provider webhook event.
 */
public enum ProviderWebhookEventStatus {
    /** Received and persisted; not yet processed. */
    RECEIVED,
    /** Processed and a financial effect (or a deliberate no-op) was applied. */
    PROCESSED,
    /** Duplicate delivery of an already-seen event; ignored. */
    DUPLICATE,
    /** Payload did not match the persisted payout record (amount/account/etc.); quarantined. */
    QUARANTINED,
    /** Authoritative provider status could not be obtained; deferred to reconciliation. */
    DEFERRED,
    /** Rejected (bad signature/timestamp); kept for audit. */
    REJECTED
}
