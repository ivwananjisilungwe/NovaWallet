package com.novawallet.novawallet_api.notification.dto;

import com.novawallet.novawallet_api.notification.entity.Notification;
import com.novawallet.novawallet_api.notification.entity.NotificationChannel;
import com.novawallet.novawallet_api.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification response DTO for the user-facing inbox.
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
                notification.getSubject(),
                notification.getMessage(),
                notification.getReadAt() != null,
                notification.getCreatedAt()
        );
    }
}
