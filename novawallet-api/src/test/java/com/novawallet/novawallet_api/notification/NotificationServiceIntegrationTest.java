package com.novawallet.novawallet_api.notification;

import com.novawallet.novawallet_api.notification.entity.*;
import com.novawallet.novawallet_api.notification.repository.NotificationAttemptRepository;
import com.novawallet.novawallet_api.notification.repository.NotificationRepository;
import com.novawallet.novawallet_api.notification.service.NotificationService;
import com.novawallet.novawallet_api.notification.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration-style tests for {@link NotificationService}.
 * <p>
 * These use mocked repositories and MailService but test real
 * NotificationService logic (creation, delivery, retry, error handling).
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceIntegrationTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationAttemptRepository attemptRepository;
    @Mock private MailService mailService;
    @Mock private SmsService smsService;

    @Captor private ArgumentCaptor<Notification> notificationCaptor;
    @Captor private ArgumentCaptor<NotificationAttempt> attemptCaptor;

    private NotificationService notificationService;
    private UUID userId;
    private Notification pendingNotification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        notificationService = new NotificationService(
                notificationRepository, attemptRepository, mailService, smsService
        );

        pendingNotification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.TRANSACTION_DEPOSIT)
                .channel(NotificationChannel.EMAIL)
                .recipient("user@example.com")
                .subject("Deposit Confirmed")
                .message("Your deposit of K100.00 was successful.")
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== sendEmail ====================

    @Test
    @DisplayName("sendEmail creates notification, delivers email, records sent status")
    void sendEmail_shouldCreateAndDeliver() {
        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .channel(NotificationChannel.EMAIL)
                .type(NotificationType.TRANSACTION_DEPOSIT)
                .recipient("user@example.com")
                .subject("Deposit")
                .message("Your deposit was successful.")
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
        doNothing().when(mailService).sendRaw(anyString(), anyString(), anyString());

        notificationService.sendEmail(userId, "user@example.com",
                NotificationType.TRANSACTION_DEPOSIT, "Deposit", "Your deposit was successful.");

        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        verify(attemptRepository).save(attemptCaptor.capture());
        verify(mailService).sendRaw("user@example.com", "Deposit", "Your deposit was successful.");

        // Final notification should be SENT
        Notification finalNotif = notificationCaptor.getAllValues().get(1);
        assertEquals(NotificationStatus.SENT, finalNotif.getStatus());
        assertNotNull(finalNotif.getSentAt());

        // Attempt should record success
        NotificationAttempt attempt = attemptCaptor.getValue();
        assertTrue(attempt.isSuccess());
        assertEquals(1, attempt.getAttemptNumber());
    }

    @Test
    @DisplayName("sendEmail records failure when MailService throws")
    void sendEmail_shouldHandleDeliveryFailure() {
        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .channel(NotificationChannel.EMAIL)
                .type(NotificationType.TRANSACTION_DEPOSIT)
                .recipient("user@example.com")
                .subject("Deposit")
                .message("Your deposit was successful.")
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(mailService).sendRaw(anyString(), anyString(), anyString());

        notificationService.sendEmail(userId, "user@example.com",
                NotificationType.TRANSACTION_DEPOSIT, "Deposit", "Your deposit was successful.");

        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        verify(attemptRepository).save(attemptCaptor.capture());

        // Status should be PENDING (retryCount 0 < maxRetries 3)
        Notification finalNotif = notificationCaptor.getAllValues().get(1);
        assertEquals(NotificationStatus.PENDING, finalNotif.getStatus());
        assertEquals(1, finalNotif.getRetryCount());
        assertNotNull(finalNotif.getLastError());

        // Attempt should record failure
        NotificationAttempt attempt = attemptCaptor.getValue();
        assertFalse(attempt.isSuccess());
        assertEquals("SMTP connection refused", attempt.getErrorMessage());
    }

    // ==================== recordSent ====================

    @Test
    @DisplayName("recordSent saves notification with SENT status without re-delivering")
    void recordSent_shouldSaveWithoutDelivery() {
        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .channel(NotificationChannel.EMAIL)
                .type(NotificationType.KYC_APPROVED)
                .recipient("user@example.com")
                .subject("KYC Approved")
                .message("Your KYC was approved.")
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        notificationService.recordSent(userId, "user@example.com", NotificationChannel.EMAIL,
                NotificationType.KYC_APPROVED, "KYC Approved", "Your KYC was approved.");

        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        verify(mailService, never()).sendRaw(anyString(), anyString(), anyString());

        Notification result = notificationCaptor.getAllValues().get(1);
        assertEquals(NotificationStatus.SENT, result.getStatus());
        assertNotNull(result.getSentAt());
    }

    // ==================== retryFailed ====================

    @Test
    @DisplayName("retryFailed retries only notifications with retryCount < maxRetries")
    void retryFailed_shouldRetryQualifyingNotifications() {
        Notification retryable = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .channel(NotificationChannel.EMAIL)
                .type(NotificationType.TRANSACTION_DEPOSIT)
                .recipient("user@example.com")
                .subject("Retry")
                .message("Retry me")
                .status(NotificationStatus.FAILED)
                .retryCount(2)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByStatusAndRetryCountLessThan(
                NotificationStatus.FAILED, 3)).thenReturn(List.of(retryable));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(mailService).sendRaw(anyString(), anyString(), anyString());

        int retried = notificationService.retryFailed();

        assertEquals(1, retried);
        verify(mailService).sendRaw("user@example.com", "Retry", "Retry me");
    }

    @Test
    @DisplayName("retryFailed marks as FAILED when max retries exhausted")
    void retryFailed_shouldMarkFailedWhenMaxRetriesExceeded() {
        Notification exhausted = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .channel(NotificationChannel.EMAIL)
                .type(NotificationType.TRANSACTION_DEPOSIT)
                .recipient("user@example.com")
                .subject("Exhausted")
                .message("I've tried")
                .status(NotificationStatus.FAILED)
                .retryCount(2)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByStatusAndRetryCountLessThan(
                NotificationStatus.FAILED, 3)).thenReturn(List.of(exhausted));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("Still failing"))
                .when(mailService).sendRaw(anyString(), anyString(), anyString());

        int retried = notificationService.retryFailed();

        assertEquals(1, retried);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification finalNotif = notificationCaptor.getValue();
        assertEquals(NotificationStatus.FAILED, finalNotif.getStatus());
        assertEquals(3, finalNotif.getRetryCount());
    }

    // ==================== sendSms ====================

    @Test
    @DisplayName("sendSms creates notification and delivers via SmsService")
    void sendSms_shouldCreateAndDeliver() {
        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .channel(NotificationChannel.SMS)
                .type(NotificationType.TRANSACTION_WITHDRAWAL)
                .recipient("+260971234567")
                .message("Withdrawal of K50.00 processed.")
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
        when(smsService.sendSms(anyString(), anyString())).thenReturn(true);

        notificationService.sendSms(userId, "+260971234567",
                NotificationType.TRANSACTION_WITHDRAWAL, "Withdrawal of K50.00 processed.");

        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        verify(attemptRepository).save(attemptCaptor.capture());
        verify(smsService).sendSms("+260971234567", "Withdrawal of K50.00 processed.");

        Notification finalNotif = notificationCaptor.getAllValues().get(1);
        assertEquals(NotificationStatus.SENT, finalNotif.getStatus());
        assertNotNull(finalNotif.getSentAt());
    }

    // ==================== sendBoth ====================

    @Test
    @DisplayName("sendBoth sends email and SMS for the same event")
    void sendBoth_shouldDeliverBothChannels() {
        Notification emailNotif = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .channel(NotificationChannel.EMAIL)
                .type(NotificationType.TRANSACTION_DEPOSIT)
                .recipient("user@example.com")
                .subject("Deposit")
                .message("Email message")
                .build();
        Notification smsNotif = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .channel(NotificationChannel.SMS)
                .type(NotificationType.TRANSACTION_DEPOSIT)
                .recipient("+260971234567")
                .message("SMS message")
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(emailNotif)
                .thenReturn(smsNotif);
        doNothing().when(mailService).sendRaw(anyString(), anyString(), anyString());
        when(smsService.sendSms(anyString(), anyString())).thenReturn(true);

        notificationService.sendBoth(userId, "user@example.com", "+260971234567",
                NotificationType.TRANSACTION_DEPOSIT,
                "Deposit", "Email message", "SMS message");

        verify(mailService).sendRaw("user@example.com", "Deposit", "Email message");
        verify(smsService).sendSms("+260971234567", "SMS message");
    }
}
