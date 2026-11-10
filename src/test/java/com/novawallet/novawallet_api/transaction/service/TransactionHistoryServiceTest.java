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
import com.novawallet.novawallet_api.wallet.entity.WalletStatus;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionHistoryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    private TransactionHistoryService service;
    private UUID userId;
    private UUID walletId;
    private Wallet wallet;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        service = new TransactionHistoryService(transactionRepository, walletRepository);

        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        wallet = Wallet.builder()
                .id(walletId)
                .userId(userId)
                .accountNumber("NW1234567890")
                .balance(BigDecimal.valueOf(500.00))
                .currency("ZMW")
                .status(WalletStatus.ACTIVE)
                .build();

        transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .reference("NWTX2026071000000001")
                .type(TransactionType.DEPOSIT)
                .amount(BigDecimal.valueOf(100.00))
                .balanceBefore(BigDecimal.valueOf(400.00))
                .balanceAfter(BigDecimal.valueOf(500.00))
                .status(TransactionStatus.SUCCESSFUL)
                .description("Test deposit")
                .receiverWalletId(walletId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== getTransactionHistory ====================

    @Test
    void shouldReturnTransactionHistory() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        Pageable pageable = PageRequest.of(0, 20);
        List<Transaction> txs = List.of(transaction);
        Page<Transaction> page = new PageImpl<>(txs, pageable, txs.size());

        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        PagedResponse<TransactionResponse> result = service.getTransactionHistory(
                walletId, userId, null, null, null, null, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).reference()).isEqualTo("NWTX2026071000000001");
        assertThat(result.content().get(0).type()).isEqualTo("DEPOSIT");
        assertThat(result.content().get(0).amount()).isEqualByComparingTo("100.00");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void shouldFilterByTransactionType() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        Pageable pageable = PageRequest.of(0, 20);
        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty());

        PagedResponse<TransactionResponse> result = service.getTransactionHistory(
                walletId, userId, TransactionType.WITHDRAWAL, null, null, null, pageable);

        assertThat(result.content()).isEmpty();
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldThrowForbiddenWhenWalletNotOwned() {
        UUID otherUserId = UUID.randomUUID();
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.getTransactionHistory(walletId, otherUserId, null, null, null, null, pageable))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Wallet does not belong");
    }

    @Test
    void shouldThrowNotFoundWhenWalletMissing() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.getTransactionHistory(walletId, userId, null, null, null, null, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }

    // ==================== getTransactionByReference ====================

    @Test
    void shouldReturnTransactionByReference() {
        when(transactionRepository.findByReference("NWTX2026071000000001"))
                .thenReturn(Optional.of(transaction));
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        TransactionResponse result = service.getTransactionByReference("NWTX2026071000000001", userId);

        assertThat(result.reference()).isEqualTo("NWTX2026071000000001");
    }

    @Test
    void shouldThrowForbiddenWhenUserNotInvolvedInTransaction() {
        UUID otherUserId = UUID.randomUUID();
        UUID otherWalletId = UUID.randomUUID();
        Wallet otherWallet = Wallet.builder()
                .id(otherWalletId)
                .userId(otherUserId)
                .accountNumber("NW9999999999")
                .balance(BigDecimal.ZERO)
                .currency("ZMW")
                .status(WalletStatus.ACTIVE)
                .build();

        // Transaction involves walletId, but user owns otherWalletId
        when(transactionRepository.findByReference("NWTX2026071000000001"))
                .thenReturn(Optional.of(transaction));
        when(walletRepository.findByUserId(otherUserId)).thenReturn(Optional.of(otherWallet));

        assertThatThrownBy(() -> service.getTransactionByReference("NWTX2026071000000001", otherUserId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Transaction does not belong");
    }

    @Test
    void shouldThrowNotFoundWhenTransactionMissing() {
        when(transactionRepository.findByReference("INVALID"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransactionByReference("INVALID", userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    // ==================== getWalletBalance ====================

    @Test
    void shouldReturnWalletBalance() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        WalletResponse result = service.getWalletBalance(walletId, userId);

        assertThat(result.balance()).isEqualByComparingTo("500.00");
        assertThat(result.currency()).isEqualTo("ZMW");
        assertThat(result.accountNumber()).isEqualTo("NW1234567890");
    }

    @Test
    void shouldThrowForbiddenWhenBalanceRequestForOtherWallet() {
        UUID otherUserId = UUID.randomUUID();
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> service.getWalletBalance(walletId, otherUserId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Wallet does not belong");
    }
}
