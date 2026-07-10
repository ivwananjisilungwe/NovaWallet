package com.novawallet.novawallet_api.fee.repository;

import com.novawallet.novawallet_api.fee.entity.FeeConfiguration;
import com.novawallet.novawallet_api.fee.enums.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeeConfigurationRepository extends JpaRepository<FeeConfiguration, UUID> {
    Optional<FeeConfiguration> findByTransactionTypeAndActiveTrue(FeeType transactionType);
}
