package com.novawallet.novawallet_api.notification.schedule;

import com.novawallet.novawallet_api.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationRetryJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryJob.class);

    private final NotificationService notificationService;

    public NotificationRetryJob(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0/15 * * * ?")
    public void retryFailedNotifications() {
        int retried = notificationService.retryFailed();
        if (retried > 0) {
            log.info("Retried {} failed notifications", retried);
        }
    }
}
