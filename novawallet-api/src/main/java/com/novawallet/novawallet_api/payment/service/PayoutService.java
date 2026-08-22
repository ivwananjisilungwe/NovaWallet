package com.novawallet.novawallet_api.payment.service;

import com.novawallet.novawallet_api.auth.service.AuthService;
import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.exception.ForbiddenException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.exception.ServiceUnavailableException;
import com.novawallet.novawallet_api.fee.enums.FeeType;
import com.novawallet.novawallet_api.fee.service.FeeEngineService;
import com.novawallet.novawallet_api.payment.PaymentGateway;
import com.novawallet.novawallet_api.payment.config.PaymentConfig;
import com.novawallet.novawallet_api.payment.dto.GatewayInitiationResponse;
import com.novawallet.novawallet_api.payment.dto.GatewayPaymentRequest;
import com.novawallet.novawallet_api.payment.dto.PayoutRequest;
import com.novawallet.novawallet_api.payment.dto.PayoutResponse;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.transaction.service.TransactionLimitService;
import com.novawallet.novawallet_api.transaction.service.TransactionReferenceGenerator;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.entity.WalletStatus;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Orchestrates mobile-money payouts: local ledger debit + gateway
 * disbursement initiation, with the same guardrails as withdrawals
 * (verified email, hourly velocity limits, PIN verification, KYC
 * daily-send-limit).
 *
 * <h2>Ordering &amp; failure semantics</h2>
 * Everything runs in one database transaction. The wallet is debited and a
 * {@code WITHDRAWAL} row is written <em>before</em> the gateway call; if the
 * provider rejects or errors the initiation, the exception rolls the debit
 * back so the user is never charged for an unaccepted payout.
 *
 * <p><strong>Known tradeoff:</strong> if the HTTP call times out after Lipila
 * actually accepted the request, we roll back locally while money may still
 * move externally — reconciliation against {@code checkStatus} is required in
 * that (rare) case and is logged loudly. A production-hardened version would
 * persist a PENDING intent first and settle asynchronously.</p>
 *
 * <p>Provider acceptance states map as: {@code Successful} → transaction
 * marked SUCCESSFUL immediately; {@code Pending} → stays PENDING until the
 * eventual callback/status poll settles it ({@code PendingTransactionCleanupJob}
 * fails stale rows); anything else → rejected, full rollback.</p>
 */
@Service
public class PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);

    private static final String STATUS_SUCCESSFUL = "Successful";
    private static final String STATUS_FAILED = "Failed";
    private static final String STATUS_PENDING = "Pending";

    private final PaymentConfig paymentConfig;
    private final PaymentGateway paymentGateway;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionReferenceGenerator referenceGenerator;
    private final TransactionLimitService transactionLimitService;
    private final FeeEngineService feeEngineService;
    private final AuthService authService;
    private final UserRepository userRepository;

    public PayoutService(
            PaymentConfig paymentConfig,
            PaymentGateway paymentGateway,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            TransactionReferenceGenerator referenceGenerator,
            TransactionLimitService transactionLimitService,
            FeeEngineService feeEngineService,
            AuthService authService,
            UserRepository userRepository
    ) {
        this.paymentConfig = paymentConfig;
        this.paymentGateway = paymentGateway;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.referenceGenerator = referenceGenerator;
        this.transactionLimitService = transactionLimitService;
        this.feeEngineService = feeEngineService;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    /**
     * Pays out {@code amount} from the user's wallet to the given mobile-money
     * account via the configured gateway.
     */
    @CacheEvict(value = "walletBalances", key = "#walletId")
    @Transactional
    public PayoutResponse payout(UUID walletId, PayoutRequest request, UUID userId) {
        requirePaymentsEnabled();

        Wallet wallet = findOwnedActiveWalletWithLock(walletId, userId);
        requireVerifiedEmail(userId);

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_EVEN);

        // Same guardrail stack as withdraw(): velocity before PIN, daily limit after.
        transactionLimitService.enforceVelocityLimits(userId, amount);
        authService.verifyPin(userId, request.pin());
        transactionLimitService.enforceDailySendLimit(userId, walletId, amount);

        BigDecimal fee = feeEngineService.calculateFee(FeeType.WITHDRAWAL, amount);
        BigDecimal totalDebit = amount.add(fee);

        BigDecimal balanceBefore = wallet.getBalance();
        int updated = walletRepository.updateBalance(walletId, totalDebit.negate());
        if (updated == 0) {
            throw new BadRequestException("Insufficient balance (including fee of " + fee + ")");
        }
        wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalStateException("Wallet disappeared after payout debit"));

        String referenceId = referencePrefix() + walletId + ":" + UUID.randomUUID();
        GatewayInitiationResponse initiation = initiateDisbursement(referenceId, request, amount);

        // Synchronous provider rejection: surface as 400 and let the enclosing
        // transaction roll back the local debit + fee rows entirely.
        // ("Pending" intentionally remains 201/PENDING — async settlement
        // arrives via the collection callback / status poll.)
        if (STATUS_FAILED.equalsIgnoreCase(initiation.status())) {
            throw new BadRequestException(
                    "Payout rejected by provider: " + initiation.message());
        }

        BigDecimal balanceAfterWithoutFee = balanceBefore.subtract(amount);
        TransactionStatus status = STATUS_SUCCESSFUL.equalsIgnoreCase(initiation.status())
                ? TransactionStatus.SUCCESSFUL
                : TransactionStatus.PENDING;

        Transaction payoutTx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfterWithoutFee)
                .status(status)
                // Embed the provider reference for reconciliation (same marker
                // convention as WebhookService's credited deposits).
                .description(request.description() + " [LIPILA:" + referenceId + "]")
                .senderWalletId(walletId)
                .build();
        payoutTx = transactionRepository.save(payoutTx);

        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            Transaction feeTx = Transaction.builder()
                    .reference(referenceGenerator.generate())
                    .type(TransactionType.FEE)
                    .amount(fee)
                    .balanceBefore(balanceAfterWithoutFee)
                    .balanceAfter(wallet.getBalance())
                    .status(status)
                    .description("Fee for payout " + payoutTx.getReference())
                    .senderWalletId(walletId)
                    .relatedTransactionId(payoutTx.getId())
                    .build();
            transactionRepository.save(feeTx);
        }

        log.info("Payout initiated: wallet={}, amount={}, fee={}, txRef={}, gatewayRef={}, gatewayStatus={}",
                walletId, amount, fee, payoutTx.getReference(), referenceId, initiation.status());

        return new PayoutResponse(
                payoutTx.getReference(),
                walletId,
                request.accountNumber(),
                amount,
                initiation.status(),
                initiation.message()
        );
    }

    // ==================== Helpers ====================

    private void requirePaymentsEnabled() {
        if (!paymentConfig.isEnabled()) {
            throw new ServiceUnavailableException(
                    "Mobile-money payouts are not available — payment gateway is disabled");
        }
    }

    private Wallet findOwnedActiveWalletWithLock(UUID walletId, UUID userId) {
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (!wallet.getUserId().equals(userId)) {
            throw new ForbiddenException("Wallet does not belong to the authenticated user");
        }
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BadRequestException("Wallet is not active");
        }
        return wallet;
    }

    private void requireVerifiedEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isEmailVerified()) {
            throw new ForbiddenException(
                    "Email not verified — please verify your email before moving funds.");
        }
    }

    private GatewayInitiationResponse initiateDisbursement(
            String referenceId, PayoutRequest request, BigDecimal amount) {
        try {
            return paymentGateway.initiateDisbursement(new GatewayPaymentRequest(
                    referenceId,
                    amount,
                    request.description(),
                    request.accountNumber(),
                    "ZMW"
            ));
        } catch (RuntimeException exception) {
            // Rolls back the local debit — see class javadoc for the timeout caveat.
            log.error("Lipila disbursement initiation failed for {}: {}",
                    referenceId, exception.getMessage(), exception);
            throw new BadRequestException(
                    "Payout could not be submitted to the mobile-money provider");
        }
    }

    /** Merchant-reference prefix matching the webhook routing convention. */
    private static String referencePrefix() {
        return "NW:";
    }
}
