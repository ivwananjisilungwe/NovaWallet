package com.novawallet.novawallet_api.transaction.enums;

/**
 * Lifecycle states for a transaction record.
 *
 * <p>{@code PENDING} — initial state for external payment flows (Flutterwave).<br>
 * {@code SUCCESSFUL} — completed successfully.<br>
 * {@code FAILED} — terminated unsuccessfully. Stale PENDING records are
 * auto-marked FAILED by a daily cleanup job.</p>
 */
public enum TransactionStatus {
    /** Initial state for external payment flows (Flutterwave). */
        PENDING,
    /** Completed successfully. */
        SUCCESSFUL,
    FAILED
}
