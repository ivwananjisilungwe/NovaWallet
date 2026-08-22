package com.novawallet.novawallet_api.transaction.service;

import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.exception.RateLimitException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.kyc.config.KycConfig;
import com.novawallet.novawallet_api.kyc.enums.KycStatus;
import com.novawallet.novawallet_api.transaction.config.TransactionVelocityConfig;
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
 * Service for enforcing transaction limits on money-movement flows.
 *
 * <p>Two layers of protection, deliberately kept separate:</p>
 * <ul>
 *   <li><strong>Velocity limits</strong> — anti-abuse guardrails (outgoing count
 *       and amount per rolling hour) applied to ALL users on withdraw/transfer.</li>
 *   <li><strong>KYC tier limits</strong> — daily-send-limit and wallet-limit,
 *       applied only to users with an approved KYC tier.</li>
 * </ul>
 *
 * <p>Centralizes limit checks so that {@link TransactionService} remains focused
 * on money-movement orchestration. All methods run inside the caller's
 * {@code @Transactional} context (REQUIRED propagation) — no additional
 * transaction boundaries are added.</p>
 */
@Service
public class TransactionLimitService {

    private static final Logger log = LoggerFactory.getLogger(TransactionLimitService.class);

    /** Rolling window for velocity checks: last 60 minutes. */
    private static final int VELOCITY_WINDOW_MINUTES = 60;

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final KycConfig kycConfig;
    private final TransactionVelocityConfig velocityConfig;

    public TransactionLimitService(
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            KycConfig kycConfig,
            TransactionVelocityConfig velocityConfig
    ) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.kycConfig = kycConfig;
        this.velocityConfig = velocityConfig;
    }

    /**
     * Enforces per-user velocity limits on outgoing money movement:
     * a rolling-hour COUNT cap and a rolling-hour AMOUNT cap.
     *
     * <p>Applies to all users regardless of KYC status — it is an abuse
     * guardrail layered beneath the tier-gated {@link #enforceDailySendLimit}.
     * Fee rows are excluded from both the count and the sum. Rejections use
     * {@link RateLimitException} (HTTP 429 RATE_LIMITED).</p>
     */
    public void enforceVelocityLimits(UUID userId, BigDecimal amount) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(VELOCITY_WINDOW_MINUTES);

        long recentCount = transactionRepository.countOutgoingByUserSince(userId, windowStart);
        if (recentCount >= velocityConfig.getMaxOutgoingCountPerHour()) {
            log.warn("Velocity count limit hit: user={}, recent={}, max={}",
                    userId, recentCount, velocityConfig.getMaxOutgoingCountPerHour());
            throw new RateLimitException(
                    "Too many outgoing transactions: maximum "
                            + velocityConfig.getMaxOutgoingCountPerHour()
                            + " per hour allowed. Please try again later."
            );
        }

        BigDecimal recentAmount = transactionRepository.sumOutgoingByUserSince(userId, windowStart);
        BigDecimal projectedTotal = recentAmount.add(amount);
        if (projectedTotal.compareTo(velocityConfig.getMaxOutgoingAmountPerHour()) > 0) {
            log.warn("Velocity amount limit hit: user={}, recent={}, requested={}, max={}",
                    userId, recentAmount, amount, velocityConfig.getMaxOutgoingAmountPerHour());
            throw new RateLimitException(
                    "Hourly outgoing amount limit of "
                            + velocityConfig.getMaxOutgoingAmountPerHour() + " exceeded. "
                            + "Sent in the last hour: " + recentAmount + ", requested: " + amount
            );
        }
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