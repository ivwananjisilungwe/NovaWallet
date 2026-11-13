package com.novawallet.novawallet_api.notification.dto;

import com.novawallet.novawallet_api.notification.entity.Notification;
import com.novawallet.novawallet_api.notification.entity.NotificationChannel;
import com.novawallet.novawallet_api.notification.entity.NotificationType;
import com.novawallet.novawallet_api.security.SensitiveDataRedactor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification response DTO for the user-facing inbox.
 * <p>
 * Message and subject are redacted defensively so no credential can reach the
 * customer-facing notification API.
 */
public record NotificationResponse(
        UUID id,
        NotificationType type,
        NotificationChannel channel,
        String subject,
        String message,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getChannel(),
                SensitiveDataRedactor.redact(notification.getSubject()),
                SensitiveDataRedactor.redact(notification.getMessage()),
                notification.getReadAt() != null,
                notification.getCreatedAt()
        );
    }
}
