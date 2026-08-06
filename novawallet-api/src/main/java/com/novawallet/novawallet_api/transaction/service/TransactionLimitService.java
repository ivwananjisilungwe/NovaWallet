package com.novawallet.novawallet_api.transaction.service;

import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.kyc.config.KycConfig;
import com.novawallet.novawallet_api.kyc.enums.KycStatus;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for enforcing KYC tier-based transaction limits.
 *
 * <p>Centralizes wallet-limit and daily-send-limit checks so that
 * {@link TransactionService} remains focused on money-movement orchestration.
 * All methods run inside the caller's {@code @Transactional} context
 * (REQUIRED propagation) — no additional transaction boundaries are added.</p>
 */
@Service
public class TransactionLimitService {

    private static final Logger log = LoggerFactory.getLogger(TransactionLimitService.class);

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final KycConfig kycConfig;

    public TransactionLimitService(
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            KycConfig kycConfig
    ) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.kycConfig = kycConfig;
    }

    /**
     * Enforces the KYC daily-send-limit for outgoing transactions (withdrawals, transfers).
     * Only enforced for users with an approved KYC tier (1+).
     * Unverified users (tier 0, not approved) are not subject to limits during MVP.
     */
    public void enforceDailySendLimit(UUID userId, UUID walletId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only enforce limits for users with approved KYC and an assigned tier
        if (user.getKycStatus() != KycStatus.APPROVED || user.getKycTier() < 1) {
            return;
        }

        KycConfig.TierConfig tier = kycConfig.getTier(user.getKycTier());
        BigDecimal dailyLimit = tier.getDailySendLimit();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        BigDecimal todaySent = transactionRepository.sumDailyOutgoing(walletId, todayStart);
        BigDecimal projectedTotal = todaySent.add(amount);

        if (projectedTotal.compareTo(dailyLimit) > 0) {
            throw new BadRequestException(
                    "Daily send limit of " + dailyLimit + " exceeded. "
                            + "Already sent: " + todaySent + ", requested: " + amount
            );
        }
    }

    /**
     * Enforces the KYC wallet-limit for deposits.
     * Only enforced for users with an approved KYC tier (1+).
     * The projected balance (current + deposit amount) must not exceed the tier's wallet limit.
     */
    public void enforceDepositWalletLimit(User user, BigDecimal amount, Wallet wallet) {
        // Only enforce limits for users with approved KYC and an assigned tier
        if (user.getKycStatus() == KycStatus.APPROVED && user.getKycTier() > 0) {
            KycConfig.TierConfig tier = kycConfig.getTier(user.getKycTier());
            BigDecimal projectedBalance = wallet.getBalance().add(amount);
            if (projectedBalance.compareTo(tier.getWalletLimit()) > 0) {
                throw new BadRequestException(
                        "Deposit would exceed KYC tier wallet limit of " + tier.getWalletLimit()
                );
            }
        }
    }
}