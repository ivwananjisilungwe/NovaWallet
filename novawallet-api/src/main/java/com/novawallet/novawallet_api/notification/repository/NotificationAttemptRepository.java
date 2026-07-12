package com.novawallet.novawallet_api.notification.repository;

import com.novawallet.novawallet_api.notification.entity.NotificationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, UUID> {

    List<NotificationAttempt> findByNotificationIdOrderByAttemptNumberAsc(UUID notificationId);
}
