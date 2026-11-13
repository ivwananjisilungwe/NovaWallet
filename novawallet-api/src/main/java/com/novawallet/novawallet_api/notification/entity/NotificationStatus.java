package com.novawallet.novawallet_api.notification.entity;

/**
 * Delivery status lifecycle for a notification.
 *
 * <p>{@code PENDING} — queued for delivery.<br>
 * {@code SENT} — delivered successfully.<br>
 * {@code FAILED} — delivery failed after exhausting retries.</p>
 */
public enum NotificationStatus {
    /** Queued for delivery. */
        PENDING,
    /** Delivered successfully. */
        SENT,
    FAILED
}
