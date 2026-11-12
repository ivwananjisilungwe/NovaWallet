package com.novawallet.novawallet_api.transaction.schedule;

import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Batch 3 (B3.4): the stale-pending sweep must never fail payouts by age —
 * payouts are routed into reconciliation instead; only legacy non-payout
 * pendings retain the historical FAILED-after-24h behavior.
 *
 * <p>Transactional so each test's seeds roll back (shared H2 database).
 * Timestamps are forced with a direct UPDATE after {@code saveAndFlush}, and
 * all post-job assertions read refreshed (DB-current) state.
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@Transactional
class PendingTransactionCleanupJobTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired private PendingTransactionCleanupJob cleanupJob;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    private UUID walletId;

    @BeforeEach
    void setUp() {
        int suffix = COUNTER.incrementAndGet();
        User user = User.builder()
                .email("cleanup-" + suffix + "@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("+260998" + String.format("%05d", suffix))
                .passwordHash("hash")
                .emailVerified(true)
                .build();
        user = userRepository.saveAndFlush(user);

        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .accountNumber("U" + String.format("%06d", suffix))
                .balance(new BigDecimal("1000.00"))
                .currency("ZMW")
                .build();
        wallet = walletRepository.saveAndFlush(wallet);
        walletId = wallet.getId();
    }

    private Transaction seed(String reference, TransactionType type, TransactionStatus status,
                             String providerReference, LocalDateTime createdAt) {
        Transaction tx = Transaction.builder()
                .reference(reference)
                .type(type)
                .amount(new BigDecimal("100.00"))
                .balanceBefore(new BigDecimal("1100.00"))
                .balanceAfter(new BigDecimal("1000.00"))
                .status(status)
                .description("seed")
                .senderWalletId(walletId)
                .providerReference(providerReference)
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

    private void runSweep() {
        cleanupJob.cleanupStalePendingTransactions();
        entityManager.flush();
    }

    @Test
    @DisplayName("B3.4: stale payout is routed to reconciliation, never marked FAILED")
    void stalePayout_routedToReconciliation_neverFailed() {
        Transaction payout = seed("NWTXCL" + String.format("%06d", COUNTER.incrementAndGet()),
                TransactionType.WITHDRAWAL, TransactionStatus.PENDING, "NW:cleanup-payout",
                LocalDateTime.now().minusHours(25));

        runSweep();

        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getStatus())
                .as("An unresolved payout must never be failed by age")
                .isEqualTo(TransactionStatus.PENDING);
        assertThat(reloaded.getNextStatusCheckAt())
                .as("Routed into reconciliation (check due now)")
                .isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(5));
    }

    @Test
    @DisplayName("B3.4: legacy non-payout pendings keep the historical FAILED-after-24h behavior")
    void staleLegacyPaying_markedFailed() {
        Transaction legacy = seed("NWTXCL" + String.format("%06d", COUNTER.incrementAndGet()),
                TransactionType.DEPOSIT, TransactionStatus.PENDING, null,
                LocalDateTime.now().minusHours(25));

        runSweep();

        assertThat(reloadTx(legacy.getId()).getStatus()).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    @DisplayName("B3.4: payouts inside the 24h window are untouched")
    void freshPayout_untouched() {
        Transaction payout = seed("NWTXCL" + String.format("%06d", COUNTER.incrementAndGet()),
                TransactionType.WITHDRAWAL, TransactionStatus.PENDING, "NW:cleanup-fresh",
                LocalDateTime.now().minusHours(2));

        runSweep();

        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(reloaded.getNextStatusCheckAt()).isNull();
    }

    @Test
    @DisplayName("B3.4: a parked payout is never un-parked by the daily sweep")
    void parkedPayout_neverUnparkedBySweep() {
        Transaction payout = seed("NWTXCL" + String.format("%06d", COUNTER.incrementAndGet()),
                TransactionType.WITHDRAWAL, TransactionStatus.PENDING, "NW:cleanup-parked",
                LocalDateTime.now().minusHours(25));
        // Same 2999-12-31 23:59 sentinel as PayoutReconciliationService.PARKED_UNTIL.
        LocalDateTime parkedSentinel = LocalDateTime.of(2999, 12, 31, 23, 59);
        jdbcTemplate.update("UPDATE transactions SET next_status_check_at = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(parkedSentinel), payout.getId());

        runSweep();

        Transaction reloaded = reloadTx(payout.getId());
        assertThat(reloaded.getNextStatusCheckAt())
                .as("The daily sweep must never un-park a payout awaiting manual investigation")
                .isEqualTo(parkedSentinel);
        assertThat(reloaded.getStatus())
                .as("A parked payout is never failed by the sweep")
                .isEqualTo(TransactionStatus.PENDING);
    }
}
