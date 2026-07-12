package com.novawallet.novawallet_api.transaction.service;

import com.novawallet.novawallet_api.audit.service.AuditService;
import com.novawallet.novawallet_api.auth.service.AuthService;
import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.exception.ForbiddenException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.fee.enums.FeeType;
import com.novawallet.novawallet_api.fee.service.FeeEngineService;
import com.novawallet.novawallet_api.kyc.config.KycConfig;
import com.novawallet.novawallet_api.kyc.enums.KycStatus;
import com.novawallet.novawallet_api.transaction.dto.DepositRequest;
import com.novawallet.novawallet_api.transaction.dto.TransactionResponse;
import com.novawallet.novawallet_api.transaction.dto.TransferRequest;
import com.novawallet.novawallet_api.transaction.dto.WithdrawRequest;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.entity.WalletStatus;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionReferenceGenerator referenceGenerator;
    private final AuthService authService;
    private final FeeEngineService feeEngineService;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final KycConfig kycConfig;

    public TransactionService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            TransactionReferenceGenerator referenceGenerator,
            AuthService authService,
            FeeEngineService feeEngineService,
            AuditService auditService,
            UserRepository userRepository,
            KycConfig kycConfig
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.referenceGenerator = referenceGenerator;
        this.authService = authService;
        this.feeEngineService = feeEngineService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.kycConfig = kycConfig;
    }

    public TransactionResponse deposit(UUID walletId, DepositRequest request, UUID userId) {
        Wallet wallet = findActiveWalletWithLock(walletId, userId);

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_EVEN);

        // Enforce KYC wallet-limit: balance after deposit must not exceed tier limit
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getKycStatus() == KycStatus.APPROVED && user.getKycTier() > 0) {
            KycConfig.TierConfig tier = kycConfig.getTier(user.getKycTier());
            BigDecimal projectedBalance = wallet.getBalance().add(amount);
            if (projectedBalance.compareTo(tier.getWalletLimit()) > 0) {
                throw new BadRequestException(
                        "Deposit would exceed KYC tier wallet limit of " + tier.getWalletLimit()
                );
            }
        }

        BigDecimal balanceBefore = wallet.getBalance();

        int updated = walletRepository.updateBalance(walletId, amount);
        if (updated == 0) {
            throw new IllegalStateException("Deposit failed — wallet may have been removed");
        }

        wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalStateException("Wallet disappeared after deposit"));

        BigDecimal balanceAfter = wallet.getBalance();

        Transaction transaction = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.SUCCESSFUL)
                .description(request.description())
                .receiverWalletId(walletId)
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Deposit: wallet={}, amount={}, reference={}, balance={}→{}",
                walletId, amount, transaction.getReference(), balanceBefore, balanceAfter);

        auditService.recordAction("Wallet", walletId, "DEPOSIT",
                balanceBefore.toString(), balanceAfter.toString(), userId);

        return toResponse(transaction);
    }

    public TransactionResponse withdraw(UUID walletId, WithdrawRequest request, UUID userId) {
        Wallet wallet = findActiveWalletWithLock(walletId, userId);

        authService.verifyPin(userId, request.pin());

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_EVEN);

        // Enforce KYC daily-send-limit for outgoing transactions
        enforceDailySendLimit(userId, walletId, amount);

        // Calculate fee
        BigDecimal fee = feeEngineService.calculateFee(FeeType.WITHDRAWAL, amount);
        BigDecimal totalDebit = amount.add(fee);

        BigDecimal balanceBefore = wallet.getBalance();

        // Single atomic debit for amount + fee
        int updated = walletRepository.updateBalance(walletId, totalDebit.negate());
        if (updated == 0) {
            throw new BadRequestException("Insufficient balance (including fee of " + fee + ")");
        }

        wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalStateException("Wallet disappeared after withdrawal"));

        BigDecimal balanceAfter = wallet.getBalance();

        // WITHDRAWAL shows balance before/after the amount deduction (fee excluded)
        BigDecimal balanceAfterWithoutFee = balanceBefore.subtract(amount);
        Transaction withdrawTx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfterWithoutFee)
                .status(TransactionStatus.SUCCESSFUL)
                .description(request.description())
                .senderWalletId(walletId)
                .build();
        withdrawTx = transactionRepository.save(withdrawTx);

        // FEE transaction shows the fee impact
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            Transaction feeTx = Transaction.builder()
                    .reference(referenceGenerator.generate())
                    .type(TransactionType.FEE)
                    .amount(fee)
                    .balanceBefore(balanceAfterWithoutFee)
                    .balanceAfter(balanceAfter)
                    .status(TransactionStatus.SUCCESSFUL)
                    .description("Fee for withdrawal " + withdrawTx.getReference())
                    .senderWalletId(walletId)
                    .relatedTransactionId(withdrawTx.getId())
                    .build();
            transactionRepository.save(feeTx);
        }

        log.info("Withdrawal: wallet={}, amount={}, fee={}, reference={}, balance={}→{}",
                walletId, amount, fee, withdrawTx.getReference(), balanceBefore, balanceAfter);

        auditService.recordAction("Wallet", walletId, "WITHDRAWAL",
                balanceBefore.toString(), balanceAfter.toString(), userId);

        return toResponse(withdrawTx);
    }

    public TransactionResponse transfer(TransferRequest request, UUID userId) {
        // Find sender wallet with pessimistic lock
        Wallet senderWallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user"));

        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BadRequestException("Sender wallet is not active");
        }

        UUID receiverWalletId = request.receiverWalletId();

        if (senderWallet.getId().equals(receiverWalletId)) {
            throw new BadRequestException("Cannot transfer to your own wallet");
        }

        // Lock sender wallet first (consistent lock order to prevent deadlocks)
        senderWallet = walletRepository.findByIdWithLock(senderWallet.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));

        // Lock receiver wallet second
        Wallet receiverWallet = walletRepository.findByIdWithLock(receiverWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found"));

        if (receiverWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BadRequestException("Receiver wallet is not active");
        }

        authService.verifyPin(userId, request.pin());

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_EVEN);

        // Enforce KYC daily-send-limit for outgoing transfers
        enforceDailySendLimit(userId, senderWallet.getId(), amount);

        // Calculate fee
        BigDecimal fee = feeEngineService.calculateFee(FeeType.TRANSFER, amount);
        BigDecimal totalSenderDebit = amount.add(fee);

        BigDecimal senderBalanceBefore = senderWallet.getBalance();

        // Single atomic debit for amount + fee
        int updated = walletRepository.updateBalance(senderWallet.getId(), totalSenderDebit.negate());
        if (updated == 0) {
            throw new BadRequestException("Insufficient balance (amount + fee = " + totalSenderDebit + ")");
        }

        // Credit receiver for amount only
        BigDecimal receiverBalanceBefore = receiverWallet.getBalance();
        int receiverUpdated = walletRepository.updateBalance(receiverWalletId, amount);
        if (receiverUpdated == 0) {
            throw new IllegalStateException(
                    "Failed to credit receiver wallet " + receiverWalletId + " — it may have been removed");
        }

        // Re-read updated balances
        senderWallet = walletRepository.findById(senderWallet.getId())
                .orElseThrow(() -> new IllegalStateException("Sender wallet disappeared"));
        receiverWallet = walletRepository.findById(receiverWalletId)
                .orElseThrow(() -> new IllegalStateException("Receiver wallet disappeared"));

        String reference = referenceGenerator.generate();

        // TRANSFER_DEBIT shows balance before/after the amount only (fee excluded)
        BigDecimal senderBalanceAfterDebit = senderBalanceBefore.subtract(amount);
        Transaction debitTx = Transaction.builder()
                .reference(reference)
                .type(TransactionType.TRANSFER_DEBIT)
                .amount(amount)
                .balanceBefore(senderBalanceBefore)
                .balanceAfter(senderBalanceAfterDebit)
                .status(TransactionStatus.SUCCESSFUL)
                .description(request.description())
                .senderWalletId(senderWallet.getId())
                .receiverWalletId(receiverWalletId)
                .build();
        debitTx = transactionRepository.save(debitTx);

        // TRANSFER_CREDIT (receiver side)
        Transaction creditTx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.TRANSFER_CREDIT)
                .amount(amount)
                .balanceBefore(receiverBalanceBefore)
                .balanceAfter(receiverWallet.getBalance())
                .status(TransactionStatus.SUCCESSFUL)
                .description(request.description())
                .senderWalletId(senderWallet.getId())
                .receiverWalletId(receiverWalletId)
                .relatedTransactionId(debitTx.getId())
                .build();
        creditTx = transactionRepository.save(creditTx);

        debitTx.setRelatedTransactionId(creditTx.getId());
        transactionRepository.save(debitTx);

        // FEE transaction if fee > 0
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            Transaction feeTx = Transaction.builder()
                    .reference(referenceGenerator.generate())
                    .type(TransactionType.FEE)
                    .amount(fee)
                    .balanceBefore(senderBalanceAfterDebit)
                    .balanceAfter(senderWallet.getBalance())
                    .status(TransactionStatus.SUCCESSFUL)
                    .description("Fee for transfer " + debitTx.getReference())
                    .senderWalletId(senderWallet.getId())
                    .relatedTransactionId(debitTx.getId())
                    .build();
            transactionRepository.save(feeTx);
        }

        log.info("Transfer: sender={}, receiver={}, amount={}, fee={}, debitRef={}, creditRef={}",
                senderWallet.getId(), receiverWalletId, amount, fee,
                debitTx.getReference(), creditTx.getReference());

        auditService.recordAction("Wallet", senderWallet.getId(), "TRANSFER_DEBIT",
                senderBalanceBefore.toString(), senderWallet.getBalance().toString(), userId);

        auditService.recordAction("Wallet", receiverWalletId, "TRANSFER_CREDIT",
                receiverBalanceBefore.toString(), receiverWallet.getBalance().toString(), userId);

        return toResponse(debitTx);
    }

    // ==================== Private helpers ====================

    /**
     * Finds a wallet with PESSIMISTIC_WRITE lock and validates ownership + active status.
     * The lock prevents concurrent balance modifications (overdraft race conditions).
     */
    private Wallet findActiveWalletWithLock(UUID walletId, UUID userId) {
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

    /**
     * Enforces the KYC daily-send-limit for outgoing transactions (withdrawals, transfers).
     * Only enforced for users with an approved KYC tier (1+).
     * Unverified users (tier 0, not approved) are not subject to limits during MVP.
     */
    private void enforceDailySendLimit(UUID userId, UUID walletId, BigDecimal amount) {
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

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getReference(),
                tx.getType().name(),
                tx.getAmount(),
                tx.getBalanceBefore(),
                tx.getBalanceAfter(),
                tx.getStatus().name(),
                tx.getDescription(),
                tx.getSenderWalletId(),
                tx.getReceiverWalletId(),
                tx.getCreatedAt()
        );
    }
}
