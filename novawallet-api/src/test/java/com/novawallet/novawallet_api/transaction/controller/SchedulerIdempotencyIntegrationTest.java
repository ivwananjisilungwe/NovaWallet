package com.novawallet.novawallet_api.transaction.controller;

import com.novawallet.novawallet_api.auth.controller.BaseAuthIntegrationTest;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.transaction.schedule.PendingTransactionCleanupJob;
import com.novawallet.novawallet_api.user.entity.Role;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that scheduled jobs are idempotent — running them twice
 * produces the same result as running them once.
 */
@SpringBootTest
class SchedulerIdempotencyIntegrationTest extends BaseAuthIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PendingTransactionCleanupJob pendingTransactionCleanupJob;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private TransactionTemplate tx;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        User u = userRepository.save(User.builder()
                .email("sched-" + UUID.randomUUID() + "@test.com")
                .passwordHash("encoded").firstName("Sched").lastName("User")
                .phone("+26098" + (System.currentTimeMillis() % 100000))
                .role(Role.USER).emailVerified(true).pinHash("$2a$10$dummy")
                .build());
        Wallet w = walletRepository.save(Wallet.builder()
                .userId(u.getId())
                .accountNumber("SCH" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .balance(BigDecimal.ZERO.setScale(2))
                .currency("ZMW")
                .build());
        walletId = w.getId();
    }

    private Transaction createPendingTransaction(String reference) {
        Transaction txn = Transaction.builder()
                .reference(reference)
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.PENDING)
                .amount(new BigDecimal("10.00"))
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(BigDecimal.ZERO)
                .senderWalletId(walletId)
                .build();
        return transactionRepository.saveAndFlush(txn);
    }

    private void backdateCreatedAt(UUID transactionId, LocalDateTime pastTime) {
        tx.executeWithoutResult(status -> {
            entityManager.createNativeQuery(
                            "UPDATE transactions SET created_at = :past WHERE id = :id")
                    .setParameter("past", java.sql.Timestamp.valueOf(pastTime))
                    .setParameter("id", transactionId)
                    .executeUpdate();
        });
    }

    @Test
    @DisplayName("PendingTransactionCleanupJob: running twice marks same count as FAILED")
    void pendingCleanupJob_shouldBeIdempotent() {
        // Create 3 PENDING transactions older than 24h
        LocalDateTime past = LocalDateTime.now().minusHours(48);
        for (int i = 0; i < 3; i++) {
            Transaction txn = createPendingTransaction("STALE-" + i);
            backdateCreatedAt(txn.getId(), past);
        }

        // Create 1 recent PENDING (should NOT be touched)
        Transaction recentTxn = createPendingTransaction("RECENT-PENDING");

        // First run
        pendingTransactionCleanupJob.cleanupStalePendingTransactions();
        long firstRunFailed = transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == TransactionStatus.FAILED)
                .count();

        // Second run — should produce same count
        pendingTransactionCleanupJob.cleanupStalePendingTransactions();
        long secondRunFailed = transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == TransactionStatus.FAILED)
                .count();

        assertThat(secondRunFailed)
                .as("Second run should FAIL the same number of transactions as the first run")
                .isEqualTo(firstRunFailed);

        // The recent PENDING transaction should still be PENDING
        Transaction recent = transactionRepository.findByReference("RECENT-PENDING").orElseThrow();
        assertThat(recent.getStatus())
                .as("Recent PENDING transaction should not be affected by cleanup")
                .isEqualTo(TransactionStatus.PENDING);
    }
}
