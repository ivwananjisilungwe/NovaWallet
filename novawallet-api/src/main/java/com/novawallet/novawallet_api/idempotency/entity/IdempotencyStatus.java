package com.novawallet.novawallet_api.idempotency.entity;

/**
 * Processing status for an idempotency key record.
 *
 * <p>{@code PROCESSING} — request is in-flight; concurrent duplicate requests are blocked.<br>
 * {@code COMPLETED} — request finished; subsequent identical keys return cached response.</p>
 */
public enum IdempotencyStatus {
    /** Request is in-flight; concurrent duplicates blocked. */
        PROCESSING,
    COMPLETED
}
