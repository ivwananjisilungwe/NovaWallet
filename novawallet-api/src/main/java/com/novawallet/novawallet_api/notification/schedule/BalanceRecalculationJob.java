package com.novawallet.novawallet_api.notification.schedule;

import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class BalanceRecalculationJob {

    private static final Logger log = LoggerFactory.getLogger(BalanceRecalculationJob.class);

    private final TransactionRepository transactionRepository;

    public BalanceRecalculationJob(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void recalculateMissingBalances() {
        int updated = transactionRepository.recalculateMissingBalances();
        if (updated > 0) {
            log.info("Balance recalculation job: fixed {} transactions with missing snapshots", updated);
        }
    }
}
