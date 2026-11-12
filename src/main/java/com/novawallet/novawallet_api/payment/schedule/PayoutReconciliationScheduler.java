package com.novawallet.novawallet_api.payment.schedule;

import com.novawallet.novawallet_api.payment.service.PayoutReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Batch 3 (B3.2): drives the payout reconciliation pass on a fixed delay.
 *
 * <p>FAIL-CLOSED: the scheduler is DISABLED unless
 * {@code app.scheduler.payout-reconciliation.enabled=true} is explicitly set.
 * {@code matchIfMissing=false} so an absent property never starts background
 * reconciliation (and therefore never polls the provider or mutates payout
 * state). The pass is safe by construction — bounded batches, provider calls
 * outside transactions, atomic claims, backoff, and a 24h escalation horizon —
 * but it must be deliberately enabled per environment. Tune via:
 * <pre>
 * app.scheduler.payout-reconciliation.enabled=true|false
 * app.scheduler.payout-reconciliation.interval-ms=60000
 * </pre>
 */
@Component
@ConditionalOnProperty(
        name = "app.scheduler.payout-reconciliation.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class PayoutReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PayoutReconciliationScheduler.class);

    private final PayoutReconciliationService reconciliationService;

    public PayoutReconciliationScheduler(PayoutReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduler.payout-reconciliation.interval-ms:60000}",
            initialDelay = 30_000
    )
    public void reconcileDuePayouts() {
        try {
            reconciliationService.reconcileDuePayouts();
        } catch (Exception e) {
            // Never let a failed reconciliation pass break the scheduler thread.
            log.error("Payout reconciliation pass failed", e);
        }
    }
}
