package com.novawallet.novawallet_api.transaction.service;

import com.novawallet.novawallet_api.common.dto.PagedResponse;
import com.novawallet.novawallet_api.exception.ForbiddenException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.transaction.dto.StatementSummaryResponse;
import com.novawallet.novawallet_api.transaction.dto.TransactionResponse;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.wallet.dto.WalletResponse;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Service for querying transaction history with filtering and pagination.
 *
 * <p>Uses Spring Data {@code Specification} for dynamic query construction.
 * Supports filtering by transaction type, status, date range. Enforces
 * ownership checks so users can only see their own wallet's transactions.</p>
 */
@Service
@Transactional(readOnly = true)
public class TransactionHistoryService {

    private static final Logger log = LoggerFactory.getLogger(TransactionHistoryService.class);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public TransactionHistoryService(
            TransactionRepository transactionRepository,
            WalletRepository walletRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    public PagedResponse<TransactionResponse> getTransactionHistory(
            UUID walletId,
            UUID userId,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        Wallet wallet = verifyWalletOwnership(walletId, userId);

        Page<Transaction> transactions = transactionRepository.findAll(
                TransactionSpecifications.withFilters(walletId, type, status, from, to),
                pageable
        );

        return PagedResponse.from(transactions.map(TransactionResponse::from));
    }

    public TransactionResponse getTransactionByReference(String reference, UUID userId) {
        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + reference));

        // User must own at least one of the involved wallets
        UUID userWalletId = walletRepository.findByUserId(userId)
                .map(Wallet::getId)
                .orElse(null);

        if (userWalletId == null ||
                (!userWalletId.equals(transaction.getSenderWalletId())
                        && !userWalletId.equals(transaction.getReceiverWalletId()))) {
            throw new ForbiddenException("Transaction does not belong to you");
        }

        return TransactionResponse.from(transaction);
    }

    public WalletResponse getWalletBalance(UUID walletId, UUID userId) {
        Wallet wallet = verifyWalletOwnership(walletId, userId);
        return WalletResponse.from(wallet);
    }

    public StatementSummaryResponse getMonthlyStatement(UUID walletId, UUID userId, YearMonth month) {
        verifyWalletOwnership(walletId, userId);

        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        List<Transaction> transactions = transactionRepository.findAll(
                TransactionSpecifications.withFilters(walletId, null, null, start, end),
                PageRequest.of(0, 100)
        ).getContent();

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        BigDecimal totalTransfersIn = BigDecimal.ZERO;
        BigDecimal totalTransfersOut = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            switch (tx.getType()) {
                case DEPOSIT -> totalDeposits = totalDeposits.add(tx.getAmount());
                case WITHDRAWAL -> totalWithdrawals = totalWithdrawals.add(tx.getAmount());
                case TRANSFER_CREDIT -> totalTransfersIn = totalTransfersIn.add(tx.getAmount());
                case TRANSFER_DEBIT -> totalTransfersOut = totalTransfersOut.add(tx.getAmount());
                case FEE -> totalFees = totalFees.add(tx.getAmount());
            }
        }

        BigDecimal netChange = totalDeposits.add(totalTransfersIn)
                .subtract(totalWithdrawals)
                .subtract(totalTransfersOut)
                .subtract(totalFees);

        return new StatementSummaryResponse(
                month.toString(),
                totalDeposits,
                totalWithdrawals,
                totalTransfersIn,
                totalTransfersOut,
                totalFees,
                netChange,
                transactions.size()
        );
    }

    // ==================== Private helpers ====================

    private Wallet verifyWalletOwnership(UUID walletId, UUID userId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (!wallet.getUserId().equals(userId)) {
            throw new ForbiddenException("Wallet does not belong to the authenticated user");
        }

        return wallet;
    }
}
