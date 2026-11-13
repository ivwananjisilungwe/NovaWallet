package com.novawallet.novawallet_api.notification.dto;

import com.novawallet.novawallet_api.notification.entity.Notification;
import com.novawallet.novawallet_api.notification.entity.NotificationChannel;
import com.novawallet.novawallet_api.notification.entity.NotificationType;
import com.novawallet.novawallet_api.security.SensitiveDataRedactor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC-03: the customer-facing notification DTO must redact subject and message
 * defensively, even when a legacy row contains a credential.
 */
class NotificationResponseTest {

    @Test
    @DisplayName("from() redacts a token embedded in the message")
    void redactsEmbeddedToken() {
        String token = "AbCdEfGh1jKlMnOpQrStUvWxYz1234567890";

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .type(NotificationType.EMAIL_VERIFICATION)
                .channel(NotificationChannel.EMAIL)
                .recipient("user@example.com")
                .subject("Token: " + token)
                .message("Your verification token is " + token)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.subject()).doesNotContain(token);
        assertThat(response.message()).doesNotContain(token);
        assertThat(response.subject()).contains("[REDACTED]");
    }

    @Test
    @DisplayName("from() is null-safe on subject and message")
    void nullSafe() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .type(NotificationType.EMAIL_VERIFICATION)
                .channel(NotificationChannel.EMAIL)
                .recipient("user@example.com")
                .subject(null)
                .message(null)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.subject()).isNull();
        assertThat(response.message()).isNull();
    }

    @Test
    @DisplayName("redact() applied to a normal message leaves it readable")
    void normalMessageUnchanged() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .type(NotificationType.TRANSACTION_DEPOSIT)
                .channel(NotificationChannel.EMAIL)
                .recipient("user@example.com")
                .subject("Deposit received")
                .message("Your deposit of 100 ZMW was successful.")
                .createdAt(LocalDateTime.now())
                .build();

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.subject()).isEqualTo("Deposit received");
        assertThat(response.message()).isEqualTo("Your deposit of 100 ZMW was successful.");
    }

    @Test
    @DisplayName("from() preserves all non-text fields")
    void preservesFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);

        Notification notification = Notification.builder()
                .id(id)
                .userId(userId)
                .type(NotificationType.EMAIL_VERIFICATION)
                .channel(NotificationChannel.EMAIL)
                .recipient("user@example.com")
                .subject("s")
                .message("m")
                .createdAt(createdAt)
                .readAt(null)
                .build();

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.type()).isEqualTo(NotificationType.EMAIL_VERIFICATION);
        assertThat(response.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(response.read()).isFalse();
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }
}
