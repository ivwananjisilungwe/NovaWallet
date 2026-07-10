package com.novawallet.novawallet_api.transaction.service;

import com.novawallet.novawallet_api.common.dto.PagedResponse;
import com.novawallet.novawallet_api.exception.ForbiddenException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

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

        return PagedResponse.from(transactions.map(this::toResponse));
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

        return toResponse(transaction);
    }

    public WalletResponse getWalletBalance(UUID walletId, UUID userId) {
        Wallet wallet = verifyWalletOwnership(walletId, userId);
        return WalletResponse.from(wallet);
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
