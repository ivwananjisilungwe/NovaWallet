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
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduled job that reaps stale in-flight transactions (Batch 3 B3.4).
 * <p>
 * Runs daily at 3:00 AM. Idempotent — running twice produces the same result
 * because terminal transactions are no longer PENDING and will not be selected.
 * <p>
 * <b>Payout rows are never marked FAILED by age.</b> An unresolved payout may
 * still have succeeded at the provider; failing it would strand customer funds.
 * Stale payouts are instead routed into the payout reconciliation scheduler
 * (their next status check is made due immediately). Only non-payout pendings
 * (legacy deposit/transfer rows) retain the historical FAILED-after-24h
 * behavior. A payout reaches FAILED only through an authoritative provider
 * outcome (with a compensating credit) — never through this job.
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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(STALE_HOURS);
        List<Transaction> stale = transactionRepository.findPendingOlderThan(cutoff);

        if (stale.isEmpty()) {
            log.info("No stale PENDING transactions found (cutoff: {})", cutoff);
            return;
        }

        List<Transaction> stalePayouts = new ArrayList<>();
        List<Transaction> staleLegacy = new ArrayList<>();
        for (Transaction tx : stale) {
            if (isPayout(tx)) {
                stalePayouts.add(tx);
            } else {
                staleLegacy.add(tx);
            }
        }

        // Payouts: route into reconciliation — never auto-failed, never auto-refunded.
        // Conditional reschedule: a payout already parked for manual investigation
        // (next_status_check_at in the far future) is skipped, so the daily sweep can
        // never un-park it and re-trigger the 24h admin alert.
        for (Transaction tx : stalePayouts) {
            transactionRepository.reschedulePayoutReconciliation(tx.getId(), now, now);
        }

        // Legacy non-payout pendings: preserve the historical behavior.
        if (!staleLegacy.isEmpty()) {
            for (Transaction tx : staleLegacy) {
                tx.setStatus(TransactionStatus.FAILED);
            }
            transactionRepository.saveAll(staleLegacy);
        }

        log.info("Stale PENDING sweep (cutoff: {}): {} payouts routed to reconciliation, {} legacy transactions marked FAILED",
                cutoff, stalePayouts.size(), staleLegacy.size());
    }

    /** A payout carries a provider reference (dedicated V14 column). */
    private static boolean isPayout(Transaction tx) {
        return tx.getProviderReference() != null && !tx.getProviderReference().isBlank();
    }
}
