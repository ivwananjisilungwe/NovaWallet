package com.novawallet.novawallet_api.wallet.repository;

import com.novawallet.novawallet_api.wallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUserId(UUID userId);

    Optional<Wallet> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    /**
     * Atomically adjusts the wallet balance by {@code amount}.
     * Positive amount = deposit, negative amount = withdrawal.
     * Enforces non-negative balance at the database level.
     *
     * @return number of rows affected (0 if wallet not found or would go negative)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithLock(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE wallets SET balance = balance + :amount WHERE id = :walletId AND balance + :amount >= 0",
            nativeQuery = true)
    int updateBalance(@Param("walletId") UUID walletId, @Param("amount") BigDecimal amount);
}
