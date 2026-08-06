package com.novawallet.novawallet_api.notification.service;

import com.novawallet.novawallet_api.notification.MailService;
import com.novawallet.novawallet_api.notification.entity.*;
import com.novawallet.novawallet_api.notification.repository.NotificationAttemptRepository;
import com.novawallet.novawallet_api.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Central notification service that coordinates email and SMS delivery
 * with persistent tracking, async delivery, and automatic retry.
 * <p>
 * Every notification is recorded in the database before delivery.
 * Failed deliveries are retried up to {@code maxRetries} times
 * (default: 3) by the {@link NotificationRetryJob}.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository attemptRepository;
    private final MailService mailService;
    private final SmsService smsService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationAttemptRepository attemptRepository,
            MailService mailService,
            SmsService smsService
    ) {
        this.notificationRepository = notificationRepository;
        this.attemptRepository = attemptRepository;
        this.mailService = mailService;
        this.smsService = smsService;
    }

    // ==================== Public API ====================

    /**
     * Send an email notification asynchronously.
     */
    @Async
    public void sendEmail(UUID userId, String email, NotificationType type, String subject, String message) {
        Notification notification = createNotification(userId, email, NotificationChannel.EMAIL, type, subject, message);
        deliver(notification);
    }

    /**
     * Send an SMS notification asynchronously.
     */
    @Async
    public void sendSms(UUID userId, String phoneNumber, NotificationType type, String message) {
        Notification notification = createNotification(userId, phoneNumber, NotificationChannel.SMS, type, null, message);
        deliver(notification);
    }

    /**
     * Send both email and SMS for the same event.
     */
    @Async
    public void sendBoth(UUID userId, String email, String phoneNumber, NotificationType type,
                         String subject, String emailMessage, String smsMessage) {
        sendEmail(userId, email, type, subject, emailMessage);
        sendSms(userId, phoneNumber, type, smsMessage);
    }

    /**
     * Record a notification as already sent without re-delivering.
     * Used when MailService has already handled delivery and we just
     * need the audit trail (e.g., verification emails, KYC notifications).
     */
    @Transactional
    public void recordSent(UUID userId, String recipient, NotificationChannel channel,
                           NotificationType type, String subject, String message) {
        Notification notification = createNotification(userId, recipient, channel, type, subject, message);
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    // ==================== Retry ====================

    /**
     * Retry failed notifications that haven't exceeded max retries.
     * Called by {@link NotificationRetryJob} on a schedule.
     */
    @Transactional
    public int retryFailed() {
        var failed = notificationRepository.findByStatusAndRetryCountLessThan(
                NotificationStatus.FAILED, 3);

        for (Notification notification : failed) {
            log.info("Retrying notification {} (attempt {}/{})",
                    notification.getId(), notification.getRetryCount() + 1, notification.getMaxRetries());
            deliver(notification);
        }
        return failed.size();
    }

    // ==================== Private ====================

    private Notification createNotification(UUID userId, String recipient, NotificationChannel channel,
                                            NotificationType type, String subject, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .recipient(recipient)
                .channel(channel)
                .type(type)
                .subject(subject)
                .message(message)
                .build();
        return notificationRepository.save(notification);
    }

    private void deliver(Notification notification) {
        boolean success;
        String error = null;

        try {
            if (notification.getChannel() == NotificationChannel.EMAIL) {
                mailService.sendRaw(notification.getRecipient(), notification.getSubject(),
                        notification.getMessage());
            } else {
                success = smsService.sendSms(notification.getRecipient(), notification.getMessage());
                if (!success) {
                    throw new RuntimeException("SMS delivery returned false");
                }
            }
            success = true;
        } catch (Exception e) {
            success = false;
            error = e.getMessage();
            log.warn("Notification delivery failed: id={}, channel={}, error={}",
                    notification.getId(), notification.getChannel(), error);
        }

        recordAttempt(notification, success, error);

        if (success) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setLastError(null);
        } else {
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setStatus(notification.getRetryCount() >= notification.getMaxRetries()
                    ? NotificationStatus.FAILED : NotificationStatus.PENDING);
            notification.setLastError(error);
        }

        notificationRepository.save(notification);
    }

    private void recordAttempt(Notification notification, boolean success, String errorMessage) {
        NotificationAttempt attempt = NotificationAttempt.builder()
                .notificationId(notification.getId())
                .attemptNumber(notification.getRetryCount() + 1)
                .success(success)
                .errorMessage(errorMessage)
                .build();
        attemptRepository.save(attempt);
    }
}
