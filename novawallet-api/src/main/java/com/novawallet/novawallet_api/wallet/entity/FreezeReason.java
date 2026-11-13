package com.novawallet.novawallet_api.wallet.entity;

/**
 * Reasons an admin may freeze a wallet.
 *
 * <p>{@code SUSPICIOUS_ACTIVITY} — unusual transaction patterns detected.<br>
 * {@code ADMIN_ACTION} — manual freeze by platform administrator.<br>
 * {@code USER_REQUEST} — user-initiated freeze request.</p>
 */
public enum FreezeReason {
    /** Unusual transaction patterns detected. */
        SUSPICIOUS_ACTIVITY,
    /** Manual freeze by platform administrator. */
        ADMIN_ACTION,
    USER_REQUEST
}
