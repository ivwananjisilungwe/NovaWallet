package com.novawallet.novawallet_api.transaction.enums;

/**
 * Lifecycle states for a transaction record.
 *
 * <p>{@code PENDING} — initial state for external payment flows (Flutterwave).<br>
 * {@code SUCCESSFUL} — completed successfully.<br>
 * {@code FAILED} — terminated unsuccessfully.<br>
 * {@code PENDING_RECONCILIATION} — provider call timed out or returned an
 * uncertain result; the wallet debit is durable and a reconciliation job or
 * status poll will resolve the final state. Stale PENDING records are
 * auto-marked FAILED by a daily cleanup job.</p>
 */
public enum TransactionStatus {
    /** Initial state for external payment flows (Flutterwave). */
        PENDING,
    /** Completed successfully. */
        SUCCESSFUL,
    /** Terminated unsuccessfully. */
        FAILED,
    /**
     * Provider call timed out or returned an uncertain result; the wallet
     * debit is durable and reconciliation will resolve the final state.
     */
        PENDING_RECONCILIATION
}
