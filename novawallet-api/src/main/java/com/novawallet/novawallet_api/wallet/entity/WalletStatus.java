package com.novawallet.novawallet_api.wallet.entity;

/**
 * Lifecycle states for a wallet.
 *
 * <p>{@code ACTIVE} — wallet is operational and can send/receive funds.<br>
 * {@code FROZEN} — wallet is locked by admin; no transactions permitted.</p>
 */
public enum WalletStatus {
    /** Wallet is operational and can send/receive funds. */
        ACTIVE,
    FROZEN
}
