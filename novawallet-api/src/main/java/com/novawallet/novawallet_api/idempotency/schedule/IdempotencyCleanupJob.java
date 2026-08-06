package com.novawallet.novawallet_api.idempotency.schedule;

import com.novawallet.novawallet_api.idempotency.repository.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduled job that cleans up expired idempotency records daily at 4:00 AM.
 *
 * Per SRS section 3.6 (S2): "Daily at 4:00 AM: delete idempotency records
 * older than 24 hours."
 *
 * This job is itself idempotent (S3): running it twice produces the same
 * result because {@code DELETE WHERE expires_at < threshold} is a safe
 * operation.
 */
@Component
public class IdempotencyCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupJob.class);

    private final IdempotencyKeyRepository repository;

    public IdempotencyCleanupJob(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanExpiredRecords() {
        LocalDateTime threshold = LocalDateTime.now();
        int deleted = repository.deleteExpired(threshold);
        if (deleted > 0) {
            log.info("Idempotency cleanup: deleted {} expired records (threshold: {})", deleted, threshold);
        }
    }
}
