package com.novawallet.novawallet_api.fee.service;

import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.fee.dto.FeeEstimateResponse;
import com.novawallet.novawallet_api.fee.entity.FeeConfiguration;
import com.novawallet.novawallet_api.fee.enums.FeeType;
import com.novawallet.novawallet_api.fee.repository.FeeConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FeeEngineService {

    private static final Logger log = LoggerFactory.getLogger(FeeEngineService.class);

    private final FeeConfigurationRepository feeConfigurationRepository;

    public FeeEngineService(FeeConfigurationRepository feeConfigurationRepository) {
        this.feeConfigurationRepository = feeConfigurationRepository;
    }

    /**
     * Calculate the fee for a given transaction type and amount.
     * Formula: totalFee = (amount * percentageFee / 100) + flatFee
     * Clamped to minFee / maxFee if configured.
     * Returns BigDecimal.ZERO if no active configuration found.
     */
    public BigDecimal calculateFee(FeeType transactionType, BigDecimal amount) {
        FeeConfiguration config = feeConfigurationRepository
                .findByTransactionTypeAndActiveTrue(transactionType)
                .orElse(null);

        if (config == null) {
            log.warn("No active fee configuration for {}", transactionType);
            return BigDecimal.ZERO;
        }

        // totalFee = (amount * percentage / 100) + flat
        BigDecimal percentagePart = amount.multiply(config.getPercentageFee())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFee = percentagePart.add(config.getFlatFee());

        // Apply min/max clamping
        if (config.getMinFee() != null && totalFee.compareTo(config.getMinFee()) < 0) {
            totalFee = config.getMinFee();
        }
        if (config.getMaxFee() != null && totalFee.compareTo(config.getMaxFee()) > 0) {
            totalFee = config.getMaxFee();
        }

        return totalFee.setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Get a detailed fee estimate including the breakdown.
     */
    public FeeEstimateResponse estimateFee(FeeType transactionType, BigDecimal amount) {
        FeeConfiguration config = feeConfigurationRepository
                .findByTransactionTypeAndActiveTrue(transactionType)
                .orElse(null);

        if (config == null) {
            return new FeeEstimateResponse(
                    transactionType.name(), amount,
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    null, null, BigDecimal.ZERO
            );
        }

        BigDecimal fee = calculateFee(transactionType, amount);

        return new FeeEstimateResponse(
                transactionType.name(), amount,
                config.getPercentageFee(), config.getFlatFee(),
                config.getMinFee(), config.getMaxFee(), fee
        );
    }

    /**
     * Get the active FeeConfiguration for a fee type.
     */
    public FeeConfiguration getActiveConfiguration(FeeType transactionType) {
        return feeConfigurationRepository
                .findByTransactionTypeAndActiveTrue(transactionType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active fee configuration for " + transactionType));
    }

    public FeeConfiguration getConfigurationById(java.util.UUID id) {
        return feeConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee configuration not found: " + id));
    }

    public FeeConfiguration updateConfiguration(java.util.UUID id, FeeConfiguration updated) {
        FeeConfiguration existing = getConfigurationById(id);
        existing.setTransactionType(updated.getTransactionType());
        existing.setPercentageFee(updated.getPercentageFee());
        existing.setFlatFee(updated.getFlatFee());
        existing.setMinFee(updated.getMinFee());
        existing.setMaxFee(updated.getMaxFee());
        existing.setActive(updated.isActive());
        return feeConfigurationRepository.save(existing);
    }

    public java.util.List<FeeConfiguration> getAllConfigurations() {
        return feeConfigurationRepository.findAll();
    }
}
