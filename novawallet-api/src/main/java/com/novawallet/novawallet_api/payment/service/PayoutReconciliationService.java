package com.novawallet.novawallet_api.payment.service;

import com.novawallet.novawallet_api.audit.service.AuditService;
import com.novawallet.novawallet_api.notification.entity.NotificationChannel;
import com.novawallet.novawallet_api.notification.entity.NotificationType;
import com.novawallet.novawallet_api.notification.service.NotificationService;
import com.novawallet.novawallet_api.payment.PaymentGateway;
import com.novawallet.novawallet_api.payment.dto.GatewayInitiationResponse;
import com.novawallet.novawallet_api.payment.dto.GatewayStatusResponse;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.user.entity.Role;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Batch 3 (B3.2): reconciles unresolved payouts against the payment provider.
 *
 * <p>Safety rules enforced here (roadmap Section 2 / Batch 3 acceptance):
 * <ul>
 *   <li>Provider status calls run OUTSIDE any database transaction.</li>
 *   <li>Only authoritative provider outcomes change payout state — and they do so
 *       through {@link PayoutService#finalizePayout}, which is idempotent and
 *       replay-guarded (compensating credit at most once).</li>
 *   <li>An unknown or failed provider CHECK never fails a payout and never
 *       refunds one: the payout is rescheduled with backoff.</li>
 *   <li>After the 24h horizon, polling stops: the payout is parked for manual
 *       investigation and admins are alerted. Funds are preserved either way.</li>
 *   <li>Bounded batches and an atomic claim keep concurrent application
 *       instances from double-polling a payout.</li>
 * </ul>
 *
 * <h2>Lease and backoff interaction</h2>
 * <p>Each pass runs against {@link LocalDateTime#now(Clock)} so tests can drive
 * time deterministically. The timing model is:
 * <ol>
 *   <li><b>Claim</b> — an atomic conditional UPDATE (optimistic {@code version}
 *       predicate, attempt cap) bumps {@code attempt_count}, pushes
 *       {@code next_status_check_at} to {@code claimTime + 5m} (the lease) and
 *       bumps {@code version}. Losing workers affect 0 rows and skip.</li>
 *   <li><b>Lease window</b> — while the claim runs, no other worker can claim the
 *       payout because its next check is in the future.</li>
 *   <li><b>Release</b> — when the attempt completes (uncertain outcome, failed
 *       check, or failed finalization), the worker releases its lease and
 *       schedules the next check at {@code claimTime + backoffMinutes(attempt)}
 *       — the documented ladder 1m, 2m, 5m, 10m, 30m, then hourly. The release
 *       is a compare-and-set on the lease value the claim wrote: a stale worker
 *       whose payout was re-claimed elsewhere affects 0 rows, and a parked
 *       payout never matches.</li>
 * </ol>
 * The retry interval between attempts is therefore the backoff step for that
 * attempt number, and the lease guarantees no two workers poll the same payout
 * inside one in-flight window.</p>
 */
@Service
public class PayoutReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(PayoutReconciliationService.class);

    /** Maximum payouts polled per scheduler run. */
    static final int BATCH_SIZE = 25;
    /** A payout younger than this may still be a live in-flight request — do not race it. */
    static final int GRACE_MINUTES = 5;
    /** Stop polling and escalate for manual investigation after this age. */
    static final int HORIZON_HOURS = 24;
    /** Lease pushed on claim so another instance does not poll the same payout. */
    static final int LEASE_MINUTES = 5;
    /** Hard cap on reconciliation attempts per payout (belt-and-braces with the horizon). */
    static final int MAX_ATTEMPTS = 96;
    /** Sentinel next-check time meaning "parked for manual investigation". */
    static final LocalDateTime PARKED_UNTIL = LocalDateTime.of(2999, 12, 31, 23, 59);

    /** Backoff ladder in minutes: 1, 2, 5, 10, 30, then hourly. */
    private static final int[] BACKOFF_MINUTES = {1, 2, 5, 10, 30};

    private final PaymentGateway paymentGateway;
    private final PayoutService payoutService;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final Clock clock;

    public PayoutReconciliationService(
            PaymentGateway paymentGateway,
            PayoutService payoutService,
            TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            AuditService auditService,
            Clock clock
    ) {
        this.paymentGateway = paymentGateway;
        this.payoutService = payoutService;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.clock = clock;
    }

    /** Outcome of one payout's reconciliation pass. */
    public enum Outcome { RESOLVED, RESCHEDULED, SKIPPED }

    /** Result summary of one scheduler run. */
    public record ReconciliationSummary(int alerted, int processed, int resolved, int rescheduled, int skipped) {}

    /**
     * One reconciliation pass. Escalates expired payouts first, then polls a
     * bounded batch of due payouts. Every provider call happens outside any
     * transaction; every state change is its own short, atomic update.
     */
    public ReconciliationSummary reconcileDuePayouts() {
        // Truncate to whole seconds: the reconciliation claim writes
        // next_status_check_at = now + leaseMinutes and the backoff release compares
        // against that stored value. Without truncation the stored TIMESTAMP and the
        // bound parameter can disagree at sub-second precision (Hibernate/H2 binding),
        // which would make the lease-ownership predicate fail for a row this worker
        // actually owns and silently leave the row on the flat 5-minute lease
        // (Phase 1B finding F1). Whole-second precision is ample for the 1m..1h
        // ladder and keeps every derived deadline exact.
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);

        int alerted = alertExpiredPayouts(now);

        int processed = 0;
        int resolved = 0;
        int rescheduled = 0;
        int skipped = 0;
        List<Transaction> due = transactionRepository.findDuePayoutsForReconciliation(
                now,
                now.minusMinutes(GRACE_MINUTES),
                now.minusHours(HORIZON_HOURS),
                PageRequest.of(0, BATCH_SIZE));
        for (Transaction tx : due) {
            Outcome outcome = processOne(tx, now);
            processed++;
            switch (outcome) {
                case RESOLVED -> resolved++;
                case RESCHEDULED -> rescheduled++;
                case SKIPPED -> skipped++;
            }
        }

        if (alerted > 0 || processed > 0) {
            log.info("Payout reconciliation pass: alerted={}, processed={}, resolved={}, rescheduled={}, skipped={}",
                    alerted, processed, resolved, rescheduled, skipped);
        }
        return new ReconciliationSummary(alerted, processed, resolved, rescheduled, skipped);
    }

    private Outcome processOne(Transaction payout, LocalDateTime now) {
        UUID txId = payout.getId();

        // Attempt counter as it was BEFORE the claim: the claim increments it by
        // one, so the post-claim value is the attempt number used for the backoff
        // ladder (attempt 1 -> 1m, 2 -> 2m, 3 -> 5m, 4 -> 10m, 5 -> 30m, then hourly).
        int preClaimAttempt = payout.getAttemptCount() == null ? 0 : payout.getAttemptCount();
        // The lease deadline the claim is about to write. It is handed to the
        // backoff update as the lease-ownership predicate.
        LocalDateTime leaseUntil = now.plusMinutes(LEASE_MINUTES);

        // Atomic claim: increments attempt_count, leases the payout and bumps
        // version. The version predicate guarantees that only the first instance to
        // claim affects a row; every concurrent instance observes claimed == 0 and
        // skips, so the provider is polled at most once per lease.
        int claimed = transactionRepository.claimPayoutForReconciliation(
                txId, now, leaseUntil, MAX_ATTEMPTS,
                payout.getVersion());
        if (claimed == 0) {
            return Outcome.SKIPPED;
        }

        // attemptCount was just incremented by the claim; use it for backoff.
        int attempt = preClaimAttempt + 1;

        UUID userId = walletRepository.findById(payout.getSenderWalletId())
                .map(Wallet::getUserId)
                .orElse(null);
        if (userId == null) {
            reschedule(txId, attempt, leaseUntil, now);
            return Outcome.RESCHEDULED;
        }

        GatewayStatusResponse providerStatus;
        try {
            // Provider call OUTSIDE any transaction.
            providerStatus = paymentGateway.checkStatus(payout.getProviderReference());
        } catch (Exception e) {
            // The check itself failed — the payout outcome is UNKNOWN. Never fail
            // it, never refund it: reschedule with backoff.
            log.warn("Payout reconciliation status check failed: tx={}, attempt={}",
                    payout.getReference(), attempt);
            reschedule(txId, attempt, leaseUntil, now);
            return Outcome.RESCHEDULED;
        }

        try {
            // Authoritative provider outcome -> Stage C finalization (idempotent,
            // replay-guarded; SUCCESS finalizes once, FAILED compensates once,
            // anything uncertain preserves the debit as PENDING_RECONCILIATION).
            // reconciliationResolution=true: the status was re-queried from the
            // provider moments ago, so a PENDING_RECONCILIATION payout may be
            // resolved; stale live-flow responses never can be.
            payoutService.finalizePayout(txId, toInitiationResponse(providerStatus), userId, true);
        } catch (Exception e) {
            log.warn("Payout reconciliation finalization failed: tx={}, attempt={}",
                    payout.getReference(), attempt);
            reschedule(txId, attempt, leaseUntil, now);
            return Outcome.RESCHEDULED;
        }

        Transaction reloaded = transactionRepository.findById(txId).orElse(null);
        if (reloaded != null
                && (reloaded.getStatus() == TransactionStatus.SUCCESSFUL
                || reloaded.getStatus() == TransactionStatus.FAILED)) {
            return Outcome.RESOLVED;
        }
        // Still unknown to the provider: back off and poll again later.
        reschedule(txId, attempt, leaseUntil, now);
        return Outcome.RESCHEDULED;
    }

    /**
     * Sweeps payouts past the 24h horizon, parks them for manual
     * investigation and alerts admins. Status and funds are left untouched.
     */
    private int alertExpiredPayouts(LocalDateTime now) {
        List<Transaction> expired = transactionRepository.findExpiredPayoutsForAlerting(
                now, now.minusHours(HORIZON_HOURS), PageRequest.of(0, BATCH_SIZE));
        int alerted = 0;
        for (Transaction payout : expired) {
            int parked = transactionRepository.parkPayoutForManualInvestigation(
                    payout.getId(), now, PARKED_UNTIL);
            if (parked == 0) {
                continue; // already parked by a concurrent run — alert already emitted
            }
            alerted++;
            String reference = payout.getReference();
            log.warn("Payout unresolved after {}h — parked for manual investigation: tx={}",
                    HORIZON_HOURS, reference);
            auditService.recordAction(
                    "Transaction", payout.getId(),
                    "PAYOUT_RECONCILIATION_ALERT", null,
                    "Payout " + reference + " unresolved after " + HORIZON_HOURS
                            + "h; parked for manual investigation (no automatic failure or refund)",
                    null);
            notifyAdmins(reference);
        }
        return alerted;
    }

    private void notifyAdmins(String payoutReference) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            notificationService.recordSent(
                    admin.getId(),
                    admin.getEmail(),
                    NotificationChannel.EMAIL,
                    NotificationType.ADMIN_ACTION,
                    "Payout reconciliation alert",
                    "Payout " + payoutReference + " is unresolved after "
                            + HORIZON_HOURS + " hours and requires manual investigation.");
        }
    }

    /**
     * Applies the backoff ladder to a payout this worker just claimed.
     *
     * <p><b>F1 correction.</b> The previous implementation called
     * {@code reschedulePayoutReconciliation}, whose predicate
     * ({@code next_status_check_at <= now}) is false the moment the claim writes a
     * future lease into that column. The call therefore affected zero rows, the row
     * stayed at the flat five-minute lease, and the documented ladder was never
     * applied. This method instead calls
     * {@link TransactionRepository#applyPayoutReconciliationBackoff}, which is
     * keyed on <em>lease ownership</em> rather than on the check being due.</p>
     *
     * <p>Ownership is expressed as {@code (attempt_count, leased_until)}: the values
     * this worker's claim produced. A later claim by another worker raises
     * {@code attempt_count} beyond {@code attempt} and therefore blocks this update,
     * so a stale worker cannot overwrite a newer lease. A parked payout carries the
     * 2999 sentinel, which exceeds {@code leasedUntil} and is likewise blocked, so a
     * parked payout can never be un-parked by a worker that claimed it earlier.</p>
     *
     * @param attempt    attempt number committed by this worker's claim (>= 1)
     * @param leasedUntil the lease deadline the claim wrote ({@code now + leaseMinutes})
     */
    private void reschedule(UUID txId, int attempt, LocalDateTime leasedUntil, LocalDateTime now) {
        LocalDateTime next = now.plusMinutes(backoffMinutes(attempt));
        int applied = transactionRepository.applyPayoutReconciliationBackoff(
                txId, attempt, leasedUntil, next);
        if (applied == 0) {
            // Contention only: the row was re-claimed, parked or finalized by someone
            // with fresher state. It keeps its existing deadline and is simply
            // revisited on a later tick. Never a payout failure, never a refund.
            if (log.isDebugEnabled()) {
                log.debug("Payout reconciliation backoff not applied (row superseded): tx={}, attempt={}",
                        txId, attempt);
            }
        }
    }

    /** Backoff ladder: 1m, 2m, 5m, 10m, 30m, then hourly. */
    static int backoffMinutes(int attempt) {
        if (attempt <= 0) {
            return BACKOFF_MINUTES[0];
        }
        if (attempt <= BACKOFF_MINUTES.length) {
            return BACKOFF_MINUTES[attempt - 1];
        }
        return 60;
    }

    private static GatewayInitiationResponse toInitiationResponse(GatewayStatusResponse status) {
        return new GatewayInitiationResponse(
                status.referenceId(),
                status.status(),
                status.message());
    }
}
