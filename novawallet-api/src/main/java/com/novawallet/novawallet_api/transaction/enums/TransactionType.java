package com.novawallet.novawallet_api.transaction.enums;

/**
 * Categorisation of financial operations.
 *
 * <p>{@code DEPOSIT} — funds added to wallet.<br>
 * {@code WITHDRAWAL} — funds removed from wallet.<br>
 * {@code TRANSFER_DEBIT} — outgoing transfer from sender's wallet.<br>
 * {@code TRANSFER_CREDIT} — incoming transfer to receiver's wallet.<br>
 * {@code FEE} — platform fee deducted from sender during transfer.</p>
 */
public enum TransactionType {
    /** Funds added to wallet. */
        DEPOSIT,
    /** Funds removed from wallet. */
        WITHDRAWAL,
    /** Outgoing transfer from sender's wallet. */
        TRANSFER_DEBIT,
    /** Incoming transfer to receiver's wallet. */
        TRANSFER_CREDIT,
    FEE
}
