package com.novawallet.novawallet_api.notification.entity;

/**
 * Delivery channels for outbound notifications.
 *
 * <p>{@code EMAIL} — sent via SMTP (Mailtrap in dev, production SMTP).<br>
 * {@code SMS} — sent via Africa's Talking API (stub in dev).</p>
 */
public enum NotificationChannel {
    /** Sent via SMTP (Mailtrap in dev, production SMTP). */
        EMAIL,
    SMS
}
