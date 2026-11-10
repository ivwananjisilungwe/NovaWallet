package com.novawallet.novawallet_api.fee.enums;

/**
 * Transaction types that trigger fee calculation.
 *
 * <p>{@code TRANSFER} — fee on peer-to-peer transfers.<br>
 * {@code WITHDRAWAL} — fee on withdrawals.<br>
 * {@code DEPOSIT} — fee on deposits (typically zero).</p>
 */
public enum FeeType {
    /** Fee on peer-to-peer transfers. */
        TRANSFER,
    /** Fee on withdrawals. */
        WITHDRAWAL,
    DEPOSIT
}
