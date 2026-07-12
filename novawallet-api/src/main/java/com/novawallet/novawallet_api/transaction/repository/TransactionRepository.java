package com.novawallet.novawallet_api.transaction.repository;

import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByReference(String reference);

    Page<Transaction> findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc(
            UUID senderWalletId, UUID receiverWalletId, Pageable pageable);

    long countByReferenceStartingWith(String prefix);

    @Query(value = "SELECT reference FROM transactions WHERE reference LIKE :prefix ORDER BY reference DESC LIMIT 1", nativeQuery = true)
    Optional<String> findMaxReferenceByPrefix(@Param("prefix") String prefix);

    /**
     * Sums the total outgoing transaction amount (WITHDRAWAL + TRANSFER_DEBIT)
     * for a wallet since the given timestamp. Used for KYC daily-send-limit enforcement.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
            + "WHERE t.senderWalletId = :walletId "
            + "AND t.type IN ('WITHDRAWAL', 'TRANSFER_DEBIT') "
            + "AND t.createdAt >= :since")
    BigDecimal sumDailyOutgoing(@Param("walletId") UUID walletId, @Param("since") LocalDateTime since);

    /**
     * Find PENDING transactions created before the given timestamp.
     * Used by {@link PendingTransactionCleanupJob} to mark stale transactions as FAILED.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.createdAt < :before")
    List<Transaction> findPendingOlderThan(@Param("before") LocalDateTime before);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE transactions t
            SET balance_before = COALESCE(
                (SELECT balance FROM wallets WHERE id = t.sender_wallet_id),
                (SELECT balance FROM wallets WHERE id = t.receiver_wallet_id)
            ),
            balance_after = COALESCE(
                (SELECT balance FROM wallets WHERE id = t.sender_wallet_id),
                (SELECT balance FROM wallets WHERE id = t.receiver_wallet_id)
            )
            WHERE t.balance_before IS NULL OR t.balance_after IS NULL
            """, nativeQuery = true)
    int recalculateMissingBalances();
}
