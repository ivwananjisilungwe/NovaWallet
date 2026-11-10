package com.novawallet.novawallet_api.transaction.schedule;

import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that marks PENDING transactions older than 24 hours as FAILED.
 * <p>
 * Runs daily at 3:00 AM. Idempotent — running twice produces the same result
 * because already-FAILED transactions are not PENDING and will not be selected.
 * <p>
 * This prevents stale "in-flight" transactions from appearing as pending
 * in user transaction history. Normal pending transactions (e.g., Flutterwave
 * deposits awaiting webhook confirmation) should complete within minutes,
 * so a 24-hour threshold is safe.
 */
@Component
public class PendingTransactionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(PendingTransactionCleanupJob.class);
    private static final int STALE_HOURS = 24;

    private final TransactionRepository transactionRepository;

    public PendingTransactionCleanupJob(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupStalePendingTransactions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(STALE_HOURS);
        List<Transaction> stale = transactionRepository.findPendingOlderThan(cutoff);

        if (stale.isEmpty()) {
            log.info("No stale PENDING transactions found (cutoff: {})", cutoff);
            return;
        }

        for (Transaction tx : stale) {
            tx.setStatus(TransactionStatus.FAILED);
        }
        transactionRepository.saveAll(stale);

        log.info("Marked {} stale PENDING transactions as FAILED (cutoff: {})", stale.size(), cutoff);
    }
}
