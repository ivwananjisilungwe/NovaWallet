package com.novawallet.novawallet_api.wallet.repository;

import com.novawallet.novawallet_api.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUserId(UUID userId);

    Optional<Wallet> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
}
