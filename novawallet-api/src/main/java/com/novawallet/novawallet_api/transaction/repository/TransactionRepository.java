package com.novawallet.novawallet_api.transaction.repository;

import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
