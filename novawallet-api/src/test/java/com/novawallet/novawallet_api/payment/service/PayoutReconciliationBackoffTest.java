package com.novawallet.novawallet_api.payment.service;

import com.novawallet.novawallet_api.auth.service.AuthService;
import com.novawallet.novawallet_api.fee.service.FeeEngineService;
import com.novawallet.novawallet_api.notification.entity.Notification;
import com.novawallet.novawallet_api.notification.entity.NotificationType;
import com.novawallet.novawallet_api.notification.repository.NotificationRepository;
import com.novawallet.novawallet_api.payment.PaymentGateway;
import com.novawallet.novawallet_api.payment.dto.GatewayStatusResponse;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.transaction.service.TransactionLimitService;
import com.novawallet.novawallet_api.user.entity.Role;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F1 correction tests: the reconciliation backoff ladder (1m, 2m, 5m, 10m, 30m,
 * then hourly) must actually be applied after each attempt, and the claim lease
 * must keep concurrent workers off the same payout.
 *
 * <p>Time is driven by a mutable {@link Clock} so every scheduled next-check
 * timestamp is asserted exactly — these tests fail against the previous
 * implementation, where the post-claim reschedule affected zero rows (its
 * “check is due” predicate was already false once the claim wrote the lease)
 * and the row silently stayed on the flat five-minute lease.</p>
 *
 * <p><b>Intentionally non-transactional</b> (mirrors
 * {@code PayoutConcurrentIdempotencyTest}): the threaded claim race needs the
 * seeded rows committed so worker threads observe them. {@link AfterEach}
 * deletes every row this class created so the shared H2 database stays clean
 * for the globally-scoped reconciliation queries used elsewhere.</p>
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@TestPropertySource(properties = "app.payments.lipila.enabled=true")
class PayoutReconciliationBackoffTest {

    private static final ZoneId ZONE = ZoneId.of("Africa/Lusaka");
    /** Fixed instant the mutable clock starts at: 2026-11-12T12:00 CAT. */
    private static final Instant T0 = Instant.parse("2026-11-12T10:00:00Z");
    /** Backoff ladder in minutes, exactly as documented. */
    private static final int[] LADDER = {1, 2, 5, 10, 30, 60};

    /** Test clock whose instant can be advanced between reconciliation passes. */
    static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant initial) {
            this.instant = initial;
        }

        MutableClock setInstant(Instant value) {
            this.instant = value;
            return this;
        }

        @Override
        public ZoneId getZone() {
            return ZONE;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @TestConfiguration
    static class MockBeansConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(T0);
        }

        @Bean
        @Primary
        PaymentGateway mockPaymentGateway() {
            return mock(PaymentGateway.class);
        }

        @Bean
        @Primary
        AuthService mockAuthService() {
            return mock(AuthService.class);
        }

        @Bean
        @Primary
        TransactionLimitService mockTransactionLimitService() {
            return mock(TransactionLimitService.class);
        }

        @Bean
        @Primary
        FeeEngineService mockFeeEngineService() {
            return mock(FeeEngineService.class);
        }
    }

    @Autowired private PayoutReconciliationService reconciliationService;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private PaymentGateway paymentGateway;
    @Autowired private FeeEngineService feeEngineService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MutableClock clock;

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private final List<UUID> createdTxIds = new ArrayList<>();
    private final List<UUID> createdUserIds = new ArrayList<>();

    private UUID walletId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        reset(paymentGateway);
        when(paymentGateway.checkStatus(any())).thenReturn(
                new GatewayStatusResponse("NW:default", "Processing", "stubbed"));
        when(feeEngineService.calculateFee(any(), any())).thenReturn(new BigDecimal("1.00"));
        clock.setInstant(T0);
        now = LocalDateTime.now(clock);

        int suffix = COUNTER.incrementAndGet();
        User user = User.builder()
                .email("backoff-" + suffix + "@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("+260995" + String.format("%05d", suffix))
                .passwordHash("hash")
                .emailVerified(true)
                .build();
        user = userRepository.saveAndFlush(user);
        createdUserIds.add(user.getId());

        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .accountNumber("B" + String.format("%06d", suffix))
                .balance(new BigDecimal("1000.00"))
                .currency("ZMW")
                .build();
        wallet = walletRepository.saveAndFlush(wallet);
        walletId = wallet.getId();
    }

    @AfterEach
    void cleanCreatedRows() {
        for (UUID id : createdTxIds) {
            jdbcTemplate.update("DELETE FROM transactions WHERE id = ?", id);
        }
        for (UUID userId : createdUserIds) {
            jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ?", userId);
        }
        createdTxIds.clear();
        createdUserIds.clear();
    }

    private Transaction seedPayout(TransactionStatus status, String providerReference,
                                   LocalDateTime createdAt, LocalDateTime nextCheckAt, int attempts) {
        int suffix = COUNTER.incrementAndGet();
        Transaction tx = Transaction.builder()
                .reference("NWTXBO" + String.format("%06d", suffix))
                .type(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("100.00"))
                .balanceBefore(new BigDecimal("1100.00"))
                .balanceAfter(new BigDecimal("1000.00"))
                .status(status)
                .description("backoff seed [LIPILA:" + providerReference + "]")
                .senderWalletId(walletId)
                .providerReference(providerReference)
                .externalStatus("Pending")
                .payoutAccount("260961234567")
                .payoutChannel("mobile-money")
                .attemptCount(attempts)
                .nextStatusCheckAt(nextCheckAt)
                .build();
        tx = transactionRepository.saveAndFlush(tx);
        createdTxIds.add(tx.getId());
        jdbcTemplate.update("UPDATE transactions SET created_at = ? WHERE id = ?", createdAt, tx.getId());
        return tx;
    }

    private Transaction reloadTx(UUID id) {
        return transactionRepository.findById(id).orElseThrow();
    }

    private BigDecimal walletBalance() {
        return walletRepository.findById(walletId).orElseThrow().getBalance();
    }

    private void reconcileOnce() {
        reconciliationService.reconcileDuePayouts();
    }

    // ==================== F1: backoff ladder ====================

    @Test
    @DisplayName("F1: first uncertain attempt schedules the next check at claim time + 1 minute")
    void firstAttempt_schedulesBackoffOneMinuteAfterClaim() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION, "NW:bo-first",
                now.minusMinutes(30), now.minusMinutes(1), 0);

        reconcileOnce();

        verify(paymentGateway, times(1)).checkStatus("NW:bo-first");
        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
        assertThat(reloaded.getNextStatusCheckAt())
                .as("Backoff step 1 is exactly one minute after the claim")
                .isEqualTo(now.plusMinutes(1));
        assertThat(reloaded.getStatus())
                .as("Unknown outcome never fails the payout")
                .isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        assertThat(walletBalance())
                .as("Uncertain payouts are never auto-refunded")
                .isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("F1: attempts 1-6 schedule 1m, 2m, 5m, 10m, 30m, 60m exactly")
    void backoffLadder_isAppliedPerAttempt() {
        List<Transaction> payouts = new ArrayList<>();
        for (int attempt = 1; attempt <= LADDER.length; attempt++) {
            payouts.add(seedPayout(TransactionStatus.PENDING_RECONCILIATION,
                    "NW:bo-ladder-" + attempt,
                    now.minusMinutes(30), now.minusMinutes(1), attempt - 1));
        }

        reconcileOnce();

        for (int attempt = 1; attempt <= LADDER.length; attempt++) {
            Transaction reloaded = reloadTx(payouts.get(attempt - 1).getId());
            assertThat(reloaded.getAttemptCount()).isEqualTo(attempt);
            assertThat(reloaded.getNextStatusCheckAt())
                    .as("Attempt %d must schedule the documented %d-minute step",
                            attempt, LADDER[attempt - 1])
                    .isEqualTo(now.plusMinutes(LADDER[attempt - 1]));
        }
        assertThat(walletBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("F1: provider check failure releases the lease with backoff; debit preserved")
    void claimThenWorkerFailure_schedulesBackoff_preservesDebit() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION, "NW:bo-fail",
                now.minusMinutes(30), now.minusMinutes(1), 0);
        when(paymentGateway.checkStatus("NW:bo-fail")).thenThrow(new RuntimeException("gateway down"));

        reconcileOnce();

        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
        assertThat(reloaded.getNextStatusCheckAt())
                .as("Failed check still schedules the first backoff step")
                .isEqualTo(now.plusMinutes(1));
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        assertThat(walletBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("F1: sequential passes with an advancing clock apply increasing backoff steps")
    void sequentialPasses_applyIncreasingBackoff() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION, "NW:bo-seq",
                now.minusMinutes(30), now.minusMinutes(1), 0);

        // Attempt 1 at T0 -> next check T0+1m
        reconcileOnce();
        assertThat(reloadTx(payout.getId()).getNextStatusCheckAt()).isEqualTo(now.plusMinutes(1));

        // Advance past the scheduled check; attempt 2 -> next check T0+1m+2m
        clock.setInstant(T0.plusSeconds(60));
        reconcileOnce();
        assertThat(reloadTx(payout.getId()).getNextStatusCheckAt())
                .isEqualTo(now.plusMinutes(1).plusMinutes(2));

        // Advance again; attempt 3 -> next check T0+3m+5m
        clock.setInstant(T0.plusSeconds(180));
        reconcileOnce();
        assertThat(reloadTx(payout.getId()).getNextStatusCheckAt())
                .isEqualTo(now.plusMinutes(1).plusMinutes(2).plusMinutes(5));

        verify(paymentGateway, times(3)).checkStatus("NW:bo-seq");
        assertThat(reloadTx(payout.getId()).getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        assertThat(walletBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    // ==================== Lease and concurrency ====================

    @Test
    @DisplayName("Acceptance: a due retry timestamp cannot invalidate an active lease")
    void lease_notInvalidatedByDueRetryTimestamp() {
        // Worker A claims at T0: the row's next check becomes the 5-minute lease
        // (T0+5m). The backoff is NOT applied yet — the attempt is still in flight.
        Transaction payout = seedPayout(TransactionStatus.PENDING, "NW:bo-lease-accept",
                now.minusMinutes(30), now.minusMinutes(1), 0);
        int claimed = transactionRepository.claimPayoutForReconciliation(
                payout.getId(), now, now.plusMinutes(5), 96, payout.getVersion());
        assertThat(claimed).as("Worker A owns the lease").isEqualTo(1);

        // Advance the clock to T0+1m — well before the 5-minute lease expires.
        // The retry/backoff timestamp is irrelevant here: the lease (T0+5m) is the
        // only gate for claiming, and it is still in the future.
        clock.setInstant(T0.plusSeconds(60));
        LocalDateTime atOneMinute = LocalDateTime.now(clock);
        assertThat(atOneMinute).isBefore(now.plusMinutes(5));

        // Worker B must NOT be able to claim: next_status_check_at (the lease,
        // T0+5m) is still in the future, so the claim predicate is false.
        int workerBClaim = transactionRepository.claimPayoutForReconciliation(
                payout.getId(), atOneMinute, atOneMinute.plusMinutes(5), 96,
                reloadTx(payout.getId()).getVersion());
        assertThat(workerBClaim)
                .as("Worker B cannot claim while Worker A's lease is active, even though 1m has elapsed")
                .isEqualTo(0);

        // The lease is untouched: still T0+5m, attempt count still 1.
        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getNextStatusCheckAt()).isEqualTo(now.plusMinutes(5));
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);

        // Only after the lease expires (T0+5m) may a new worker claim.
        clock.setInstant(T0.plusSeconds(300));
        LocalDateTime atExpiry = LocalDateTime.now(clock);
        int reclaimed = transactionRepository.claimPayoutForReconciliation(
                payout.getId(), atExpiry, atExpiry.plusMinutes(5), 96,
                reloadTx(payout.getId()).getVersion());
        assertThat(reclaimed).as("Lease expiry permits recovery by another worker").isEqualTo(1);
    }

    @Test
    @DisplayName("Lease: an in-flight claim blocks other workers until the lease expires")
    void leaseWindow_blocksSecondWorkerUntilExpiry() {
        Transaction payout = seedPayout(TransactionStatus.PENDING, "NW:bo-lease",
                now.minusMinutes(30), now.minusMinutes(1), 0);

        // Simulate worker A's claim at T0 (writes attempt=1, lease = T0+5m, version=1).
        int claimed = transactionRepository.claimPayoutForReconciliation(
                payout.getId(), now, now.plusMinutes(5), 96, payout.getVersion());
        assertThat(claimed).isEqualTo(1);

        // A pass at T0 must not poll: the row's next check is the lease (future).
        reconcileOnce();
        verify(paymentGateway, never()).checkStatus("NW:bo-lease");

        // Once the lease expires and the clock passes it, the payout is due again.
        clock.setInstant(T0.plusSeconds(300));
        reconcileOnce();
        verify(paymentGateway, times(1)).checkStatus("NW:bo-lease");
    }

    @Test
    @DisplayName("Lease: a stale worker cannot reschedule over a newer lease")
    void staleWorker_cannotRescheduleOverNewerLease() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION, "NW:bo-stale",
                now.minusMinutes(30), now.minusMinutes(1), 0);

        // Worker A claims at T0: attempt=1, lease T0+5m, version 0->1.
        assertThat(transactionRepository.claimPayoutForReconciliation(
                payout.getId(), now, now.plusMinutes(5), 96, payout.getVersion())).isEqualTo(1);

        // A is slow; its lease expires and worker B re-claims at T0+5m (version=1):
        // attempt=2, lease T0+10m.
        LocalDateTime later = now.plusMinutes(5);
        assertThat(transactionRepository.claimPayoutForReconciliation(
                payout.getId(), later, later.plusMinutes(5), 96, 1)).isEqualTo(1);

        // A finally finishes and tries to release ITS lease (attempt 1, lease T0+5m):
        // the row no longer matches, so the update affects 0 rows.
        int applied = transactionRepository.applyPayoutReconciliationBackoff(
                payout.getId(), 1, now.plusMinutes(5), now.plusMinutes(1));
        assertThat(applied).as("Stale worker must not overwrite the newer lease").isEqualTo(0);

        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getNextStatusCheckAt())
                .as("Worker B's lease is intact")
                .isEqualTo(later.plusMinutes(5));
        assertThat(reloaded.getAttemptCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Concurrency: two workers racing on the same due payout poll it once")
    void concurrentWorkers_providerPolledOnce() throws Exception {
        seedPayout(TransactionStatus.PENDING_RECONCILIATION, "NW:bo-race",
                now.minusMinutes(30), now.minusMinutes(1), 0);

        CountDownLatch startGate = new CountDownLatch(2);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable pass = () -> {
            try {
                startGate.countDown();
                startGate.await(5, TimeUnit.SECONDS);
                reconciliationService.reconcileDuePayouts();
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        };

        Thread t1 = new Thread(pass, "reconcile-1");
        Thread t2 = new Thread(pass, "reconcile-2");
        t1.start();
        t2.start();
        t1.join(30_000);
        t2.join(30_000);

        assertThat(failure.get()).as("Both reconciliation passes must resolve").isNull();
        verify(paymentGateway, times(1)).checkStatus("NW:bo-race");
        assertThat(walletBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    // ==================== Parking and alerting ====================

    @Test
    @DisplayName("Parking: a parked payout is never un-parked or polled again")
    void parkedPayout_notUnparked_notPolled() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION, "NW:bo-parked",
                now.minusHours(25), PayoutReconciliationService.PARKED_UNTIL, 5);

        reconcileOnce();

        verify(paymentGateway, never()).checkStatus("NW:bo-parked");
        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getNextStatusCheckAt())
                .as("Parked payouts remain parked")
                .isEqualTo(PayoutReconciliationService.PARKED_UNTIL);
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        assertThat(walletBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Alerting: a payout past the 24h horizon is parked and alerted exactly once")
    void expiredPayout_parkedOnce_alertedOnce() {
        int suffix = COUNTER.incrementAndGet();
        User admin = User.builder()
                .email("backoff-admin-" + suffix + "@example.com")
                .firstName("Admin")
                .lastName("User")
                .phone("+260997" + String.format("%05d", suffix))
                .passwordHash("hash")
                .emailVerified(true)
                .role(Role.ADMIN)
                .build();
        admin = userRepository.saveAndFlush(admin);
        createdUserIds.add(admin.getId());

        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION, "NW:bo-expired",
                now.minusHours(25), now.minusMinutes(1), 5);

        reconcileOnce();
        reconcileOnce();

        verify(paymentGateway, never()).checkStatus("NW:bo-expired");
        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getStatus())
                .as("Unresolved payouts are never failed by the system")
                .isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        assertThat(reloaded.getNextStatusCheckAt())
                .as("Parked for manual investigation")
                .isEqualTo(PayoutReconciliationService.PARKED_UNTIL);

        List<Notification> alerts = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(admin.getId(), PageRequest.of(0, 10))
                .getContent();
        assertThat(alerts)
                .as("Exactly one admin alert per parked payout")
                .filteredOn(n -> n.getType() == NotificationType.ADMIN_ACTION)
                .hasSize(1);
        assertThat(walletBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }
}
