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
import com.novawallet.novawallet_api.transaction.schedule.PendingTransactionCleanupJob;
import com.novawallet.novawallet_api.transaction.service.TransactionLimitService;
import com.novawallet.novawallet_api.user.entity.Role;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import jakarta.persistence.EntityManager;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Batch 3 (B3.2): payout reconciliation scheduler behaviour — authoritative
 * resolution, unknown-outcome backoff, in-flight grace, and the 24h
 * park-and-alert horizon. Unknown payouts are never failed or refunded.
 *
 * <p>Transactional so each test's seeds roll back (shared H2 database; the
 * reconciliation queries are global by design in the single-tenant production
 * model). Timestamps are forced with a direct UPDATE after {@code
 * saveAndFlush}; all post-service assertions read refreshed (DB-current)
 * state; provider-call assertions are reference-targeted.
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@TestPropertySource(properties = "app.payments.lipila.enabled=true")
@Transactional
class PayoutReconciliationServiceTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @TestConfiguration
    static class MockBeansConfig {
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
    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private PaymentGateway paymentGateway;
    @Autowired private FeeEngineService feeEngineService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    private UUID userId;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        reset(paymentGateway);
        // Default stub: any payout polled during a test (including rows seeded
        // by other, non-transactional test classes in the shared H2 database)
        // gets a deterministic uncertain outcome instead of a null NPE path.
        when(paymentGateway.checkStatus(any()))
                .thenReturn(new GatewayStatusResponse("NW:default", "Pending", "stubbed"));
        when(feeEngineService.calculateFee(any(), any())).thenReturn(new BigDecimal("1.00"));
        userId = UUID.randomUUID();
        int suffix = COUNTER.incrementAndGet();

        User user = User.builder()
                .email("recon-" + suffix + "@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("+260996" + String.format("%05d", suffix))
                .passwordHash("hash")
                .emailVerified(true)
                .build();
        user = userRepository.saveAndFlush(user);
        userId = user.getId();

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .accountNumber("R" + String.format("%06d", suffix))
                .balance(new BigDecimal("1000.00"))
                .currency("ZMW")
                .build();
        wallet = walletRepository.saveAndFlush(wallet);
        walletId = wallet.getId();
    }

    private Transaction seedPayout(TransactionStatus status, String providerReference,
                                   LocalDateTime createdAt, LocalDateTime nextCheckAt, int attempts) {
        int suffix = COUNTER.incrementAndGet();
        Transaction tx = Transaction.builder()
                .reference("NWTXR" + String.format("%06d", suffix))
                .type(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("100.00"))
                .balanceBefore(new BigDecimal("1100.00"))
                .balanceAfter(new BigDecimal("1000.00"))
                .status(status)
                .description("Payout [ACCT:R000001] [LIPILA:" + providerReference + "]")
                .senderWalletId(walletId)
                .providerReference(providerReference)
                .externalStatus("Pending")
                .payoutAccount("260961234567")
                .payoutChannel("mobile-money")
                .attemptCount(attempts)
                .nextStatusCheckAt(nextCheckAt)
                .build();
        // saveAndFlush FIRST: the timestamp UPDATE below is plain JDBC and must
        // find the row (inside a transactional test the INSERT is otherwise
        // still pending in the persistence context).
        tx = transactionRepository.saveAndFlush(tx);
        jdbcTemplate.update("UPDATE transactions SET created_at = ? WHERE id = ?", createdAt, tx.getId());
        return tx;
    }

    private Transaction reloadTx(UUID id) {
        Transaction tx = transactionRepository.findById(id).orElseThrow();
        entityManager.refresh(tx);
        return tx;
    }

    private BigDecimal walletBalance() {
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        entityManager.refresh(wallet);
        return wallet.getBalance();
    }

    /**
     * Runs one reconciliation pass and flushes: inside this transactional test,
     * plain JPA saves stay deferred until flush, so refreshed assertions would
     * otherwise read stale rows. Production commits flush normally.
     */
    private void reconcileOnce() {
        reconciliationService.reconcileDuePayouts();
        entityManager.flush();
    }

    @Test
    @DisplayName("B3.2: due PENDING_RECONCILIATION payout polls provider and finalizes on Successful")
    void duePayout_authoritativeSuccess_finalizes() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION, "NW:recon-success",
                LocalDateTime.now().minusMinutes(30), LocalDateTime.now().minusMinutes(1), 0);
        when(paymentGateway.checkStatus("NW:recon-success"))
                .thenReturn(new GatewayStatusResponse("NW:recon-success", "Successful", "Paid"));

        reconcileOnce();

        verify(paymentGateway, times(1)).checkStatus("NW:recon-success");
        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.SUCCESSFUL);
        assertThat(reloaded.getExternalStatus()).isEqualTo("Successful");
    }

    @Test
    @DisplayName("B3.2: authoritative Failed finalizes with a single compensating credit")
    void duePayout_authoritativeFailure_compensatesOnce() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION, "NW:recon-failed",
                LocalDateTime.now().minusMinutes(30), LocalDateTime.now().minusMinutes(1), 0);
        when(paymentGateway.checkStatus("NW:recon-failed"))
                .thenReturn(new GatewayStatusResponse("NW:recon-failed", "Failed", "Rejected"));

        reconcileOnce();

        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.FAILED);
        long deposits = transactionRepository.findAll().stream()
                .filter(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.DEPOSIT)
                .count();
        assertThat(deposits).as("Exactly one compensating credit").isEqualTo(1);
        assertThat(walletBalance()).isEqualByComparingTo(new BigDecimal("1101.00"));
    }

    @Test
    @DisplayName("B3.2: unknown provider status preserves the debit and reschedules with backoff")
    void unknownProviderStatus_reschedules_preservesDebit() {
        Transaction payout = seedPayout(TransactionStatus.PENDING, "NW:recon-unknown",
                LocalDateTime.now().minusMinutes(30), LocalDateTime.now().minusMinutes(1), 0);
        when(paymentGateway.checkStatus("NW:recon-unknown"))
                .thenReturn(new GatewayStatusResponse("NW:recon-unknown", "Processing", "Processing"));

        reconcileOnce();

        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getStatus())
                .as("Unknown outcome must not fail the payout")
                .isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
        assertThat(reloaded.getNextStatusCheckAt())
                .as("Rescheduled ~1 minute out (first backoff step)")
                .isAfter(LocalDateTime.now().minusSeconds(5));

        assertThat(walletBalance())
                .as("Funds preserved — never auto-refunded")
                .isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("B3.2: provider check failure reschedules without touching status or funds")
    void providerCheckFailure_reschedules_noStateChange() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION, "NW:recon-error",
                LocalDateTime.now().minusMinutes(30), LocalDateTime.now().minusMinutes(1), 0);
        when(paymentGateway.checkStatus("NW:recon-error"))
                .thenThrow(new RuntimeException("gateway unreachable"));

        reconcileOnce();

        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
        assertThat(reloaded.getNextStatusCheckAt()).isAfter(LocalDateTime.now().minusSeconds(5));
        assertThat(walletBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("B3.2: payouts inside the in-flight grace window are not polled")
    void livePayoutWithinGrace_notProcessed() {
        seedPayout(TransactionStatus.PENDING, "NW:recon-live",
                LocalDateTime.now().minusSeconds(30), null, 0);

        reconcileOnce();

        verify(paymentGateway, never()).checkStatus("NW:recon-live");
    }

    @Test
    @DisplayName("B3.2: payout past the 24h horizon is parked and alerted — never failed, never refunded")
    void expiredPayout_parkedAndAlerted_neverFailed() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION, "NW:recon-expired",
                LocalDateTime.now().minusHours(25), LocalDateTime.now().minusMinutes(1), 5);
        User admin = User.builder()
                .email("recon-admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .phone("+26099700001")
                .passwordHash("hash")
                .emailVerified(true)
                .role(Role.ADMIN)
                .build();
        admin = userRepository.saveAndFlush(admin);

        reconcileOnce();

        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getStatus())
                .as("An unresolved payout is never marked FAILED by the system")
                .isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        assertThat(reloaded.getNextStatusCheckAt())
                .as("Parked for manual investigation").isEqualTo(PayoutReconciliationService.PARKED_UNTIL);

        verify(paymentGateway, never()).checkStatus("NW:recon-expired");

        List<Notification> adminNotifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(admin.getId(), PageRequest.of(0, 10))
                        .getContent();
        assertThat(adminNotifications)
                .as("Admins are alerted for manual investigation")
                .anyMatch(n -> n.getType() == NotificationType.ADMIN_ACTION);
    }

    @Test
    @DisplayName("B3.2: repeated uncertain reconciliation does not churn status, description or funds")
    void repeatedReconciliation_uncertain_noChurn() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION, "NW:recon-repeat",
                LocalDateTime.now().minusMinutes(30), LocalDateTime.now().minusMinutes(1), 0);
        when(paymentGateway.checkStatus("NW:recon-repeat"))
                .thenReturn(new GatewayStatusResponse("NW:recon-repeat", "Pending", "Accepted"));

        reconcileOnce();
        reconcileOnce();

        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        assertThat(reloaded.getDescription())
                .as("No duplicate finalization markers from repeated uncertain passes")
                .doesNotContain("marked PENDING_RECONCILIATION");
        assertThat(walletBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("B3.2: backoff ladder is 1, 2, 5, 10, 30 minutes then hourly")
    void backoffLadder_matchesPlan() {
        assertThat(PayoutReconciliationService.backoffMinutes(1)).isEqualTo(1);
        assertThat(PayoutReconciliationService.backoffMinutes(2)).isEqualTo(2);
        assertThat(PayoutReconciliationService.backoffMinutes(3)).isEqualTo(5);
        assertThat(PayoutReconciliationService.backoffMinutes(4)).isEqualTo(10);
        assertThat(PayoutReconciliationService.backoffMinutes(5)).isEqualTo(30);
        assertThat(PayoutReconciliationService.backoffMinutes(6)).isEqualTo(60);
        assertThat(PayoutReconciliationService.backoffMinutes(50)).isEqualTo(60);
    }

    // ==================== F1: backoff is actually applied (not the 5m lease) ====================

    @Test
    @DisplayName("F1: after a claim the scheduled next check is the backoff step, not the 5m lease")
    void f1_backoffApplied_notLease() {
        // Seed one payout per attempt level up front so a single pass exercises the
        // whole ladder. Each is due (next check in the past) and inside the horizon.
        List<Transaction> payouts = new java.util.ArrayList<>();
        for (int k = 0; k < 6; k++) {
            payouts.add(seedPayout(TransactionStatus.PENDING_RECONCILIATION,
                    "NW:f1-ladder-" + k,
                    LocalDateTime.now().minusMinutes(30),
                    LocalDateTime.now().minusMinutes(1),
                    k)); // attempt_count = k -> attempt after claim = k+1
        }

        LocalDateTime startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        reconcileOnce();
        LocalDateTime finishedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        int[] expectedSteps = {1, 2, 5, 10, 30, 60};
        for (int k = 0; k < 6; k++) {
            Transaction reloaded = reloadTx(payouts.get(k).getId());
            int expectedBackoff = expectedSteps[k];
            assertThat(reloaded.getNextStatusCheckAt())
                    .as("attempt %d -> backoff %d minutes (not the 5m lease)", k + 1, expectedBackoff)
                    .isBetween(startedAt.plusMinutes(expectedBackoff), finishedAt.plusMinutes(expectedBackoff).plusSeconds(1));
            // Defensive: must be strictly earlier than the flat 5-minute lease would be
            // for the short steps (1m, 2m) — proving the lease was not left in place.
            if (expectedBackoff < PayoutReconciliationService.LEASE_MINUTES) {
                assertThat(reloaded.getNextStatusCheckAt())
                        .as("short backoff step must not be the 5-minute lease")
                        .isBefore(startedAt.plusMinutes(PayoutReconciliationService.LEASE_MINUTES));
            }
        }
    }

    @Test
    @DisplayName("F1: two workers racing to claim the same payout — only one wins")
    void f1_concurrentClaim_onlyOneWins() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION,
                "NW:f1-race",
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().minusMinutes(1),
                0);
        LocalDateTime now = LocalDateTime.now();

        int first = transactionRepository.claimPayoutForReconciliation(
                payout.getId(), now, now.plusMinutes(PayoutReconciliationService.LEASE_MINUTES),
                PayoutReconciliationService.MAX_ATTEMPTS, payout.getVersion());
        int second = transactionRepository.claimPayoutForReconciliation(
                payout.getId(), now, now.plusMinutes(PayoutReconciliationService.LEASE_MINUTES),
                PayoutReconciliationService.MAX_ATTEMPTS, payout.getVersion());

        assertThat(first).as("first claim wins").isEqualTo(1);
        assertThat(second).as("second claim loses (optimistic version)").isEqualTo(0);
    }

    @Test
    @DisplayName("F1: a stale worker (wrong attempt/lease) cannot reschedule")
    void f1_staleWorker_cannotReschedule() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION,
                "NW:f1-stale",
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().minusMinutes(1),
                0);
        LocalDateTime now = LocalDateTime.now();
        int claimed = transactionRepository.claimPayoutForReconciliation(
                payout.getId(), now, now.plusMinutes(PayoutReconciliationService.LEASE_MINUTES),
                PayoutReconciliationService.MAX_ATTEMPTS, payout.getVersion());
        assertThat(claimed).isEqualTo(1);

        // Simulate another worker re-claiming: bump attempt_count + lease in the DB.
        jdbcTemplate.update(
                "UPDATE transactions SET attempt_count = 2, next_status_check_at = ? WHERE id = ?",
                now.plusMinutes(10), payout.getId());

        // Stale worker (attempt=1, old lease) tries to apply its backoff.
        int applied = transactionRepository.applyPayoutReconciliationBackoff(
                payout.getId(), 1, now.plusMinutes(5), now.plusMinutes(1));
        assertThat(applied).as("stale worker affects 0 rows").isEqualTo(0);

        // The lease written by the re-claim is untouched (compare with second
        // precision: the database TIMESTAMP column does not preserve nanoseconds).
        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getNextStatusCheckAt())
                .isCloseTo(now.plusMinutes(10), within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("F1: a parked payout cannot be unparked by reconciliation or cleanup")
    void f1_parkedPayout_staysParked() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION,
                "NW:f1-parked",
                LocalDateTime.now().minusHours(25),
                LocalDateTime.now().minusMinutes(1),
                5);
        // Park it first.
        int parked = transactionRepository.parkPayoutForManualInvestigation(
                payout.getId(), LocalDateTime.now(), PayoutReconciliationService.PARKED_UNTIL);
        assertThat(parked).isEqualTo(1);

        // Reconciliation pass must not un-park it.
        reconcileOnce();
        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getNextStatusCheckAt())
                .as("parked payout keeps the 2999 sentinel")
                .isEqualTo(PayoutReconciliationService.PARKED_UNTIL);

        // Cleanup job (routes stale payouts into reconciliation) must not un-park it.
        new PendingTransactionCleanupJob(transactionRepository)
                .cleanupStalePendingTransactions();
        entityManager.flush();
        Transaction reloaded2 = reloadTx(payout.getId());
        assertThat(reloaded2.getNextStatusCheckAt())
                .as("cleanup job cannot un-park a parked payout")
                .isEqualTo(PayoutReconciliationService.PARKED_UNTIL);
    }

    @Test
    @DisplayName("F1: only one admin alert is emitted per parked payout across passes")
    void f1_parkedPayout_singleAlert() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION,
                "NW:f1-alert",
                LocalDateTime.now().minusHours(25),
                LocalDateTime.now().minusMinutes(1),
                5);
        User admin = User.builder()
                .email("f1-alert-admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .phone("+26099700099")
                .passwordHash("hash")
                .emailVerified(true)
                .role(Role.ADMIN)
                .build();
        admin = userRepository.saveAndFlush(admin);

        reconcileOnce();
        reconcileOnce();

        List<Notification> adminNotifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(admin.getId(), PageRequest.of(0, 10))
                        .getContent();
        long alerts = adminNotifications.stream()
                .filter(n -> n.getType() == NotificationType.ADMIN_ACTION)
                .count();
        assertThat(alerts).as("exactly one admin alert per parked payout").isEqualTo(1);
    }

    @Test
    @DisplayName("F1: lease expiration allows a later claim")
    void f1_leaseExpiry_allowsReclaim() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION,
                "NW:f1-lease",
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().plusMinutes(10), // currently leased into the future
                0);
        // Simulate lease expiry by pushing next_status_check_at into the past.
        jdbcTemplate.update(
                "UPDATE transactions SET next_status_check_at = ? WHERE id = ?",
                LocalDateTime.now().minusMinutes(1), payout.getId());

        LocalDateTime now = LocalDateTime.now();
        int claimed = transactionRepository.claimPayoutForReconciliation(
                payout.getId(), now, now.plusMinutes(PayoutReconciliationService.LEASE_MINUTES),
                PayoutReconciliationService.MAX_ATTEMPTS,
                reloadTx(payout.getId()).getVersion());
        assertThat(claimed).as("expired lease -> claim succeeds").isEqualTo(1);
    }

    @Test
    @DisplayName("F1: claim followed by worker failure still schedules the backoff")
    void f1_workerFailure_schedulesBackoff() {
        Transaction payout = seedPayout(TransactionStatus.PENDING_RECONCILIATION,
                "NW:f1-fail",
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().minusMinutes(1),
                0);
        when(paymentGateway.checkStatus("NW:f1-fail"))
                .thenThrow(new RuntimeException("gateway unreachable"));

        LocalDateTime startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        reconcileOnce();

        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
        assertThat(reloaded.getNextStatusCheckAt())
                .as("worker failure -> first backoff step (1m), not the 5m lease")
                .isBetween(startedAt.plusMinutes(1), LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).plusMinutes(1).plusSeconds(1));
        assertThat(reloaded.getNextStatusCheckAt())
                .isBefore(startedAt.plusMinutes(PayoutReconciliationService.LEASE_MINUTES));
    }
}

