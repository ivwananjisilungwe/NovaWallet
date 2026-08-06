package com.novawallet.novawallet_api.notification.entity;

/**
 * Categories of notification events that trigger outbound messages.</p>
 */
public enum NotificationType {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    /** Notification that KYC verification was approved. */
        KYC_APPROVED,
    /** Notification that KYC verification was rejected. */
        KYC_REJECTED,
    /** Notification triggered by a deposit event. */
        TRANSACTION_DEPOSIT,
    /** Notification triggered by a withdrawal event. */
        TRANSACTION_WITHDRAWAL,
    /** Notification to sender when a transfer is completed. */
        TRANSACTION_TRANSFER_SENT,
    /** Notification to receiver when a transfer is received. */
        TRANSACTION_TRANSFER_RECEIVED,
    WALLET_FROZEN,
    WALLET_UNFROZEN,
    ADMIN_ACTION
}
