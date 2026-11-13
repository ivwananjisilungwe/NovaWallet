package com.novawallet.novawallet_api.transaction.service;

import com.novawallet.novawallet_api.auth.service.AuthService;
import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.fee.enums.FeeType;
import com.novawallet.novawallet_api.fee.service.FeeEngineService;
import com.novawallet.novawallet_api.notification.service.NotificationService;
import com.novawallet.novawallet_api.security.EmailVerificationPolicy;
import com.novawallet.novawallet_api.transaction.dto.TransferRequest;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.entity.WalletStatus;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTransferLockOrderTest {

    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionReferenceGenerator referenceGenerator;
    @Mock private AuthService authService;
    @Mock private FeeEngineService feeEngineService;
    @Mock private com.novawallet.novawallet_api.audit.service.AuditService auditService;
    @Mock private UserRepository userRepository;
    @Mock private TransactionLimitService transactionLimitService;
    @Mock private NotificationService notificationService;
    @Mock private EmailVerificationPolicy emailVerificationPolicy;

    private TransactionService newService() {
        return new TransactionService(
                walletRepository, transactionRepository, referenceGenerator,
                authService, feeEngineService, auditService, userRepository,
                transactionLimitService, notificationService, emailVerificationPolicy);
    }

    private void runDirection(UUID senderId, UUID receiverId, UUID userId) {
        Wallet senderWallet = Wallet.builder()
                .id(senderId).userId(userId).accountNumber("S")
                .balance(new BigDecimal("100.00")).status(WalletStatus.ACTIVE).build();
        Wallet receiverWallet = Wallet.builder()
                .id(receiverId).userId(UUID.randomUUID()).accountNumber("R")
                .balance(BigDecimal.ZERO.setScale(2)).status(WalletStatus.ACTIVE).build();

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(senderWallet));

        UUID lower = senderId.compareTo(receiverId) < 0 ? senderId : receiverId;
        UUID higher = senderId.compareTo(receiverId) < 0 ? receiverId : senderId;

        Wallet lowerWallet = lower.equals(senderId) ? senderWallet : receiverWallet;
        Wallet higherWallet = higher.equals(senderId) ? senderWallet : receiverWallet;

        when(walletRepository.findByIdWithLock(lower)).thenReturn(Optional.of(lowerWallet));
        when(walletRepository.findByIdWithLock(higher)).thenReturn(Optional.of(higherWallet));

        when(feeEngineService.calculateFee(FeeType.TRANSFER, new BigDecimal("20.00")))
                .thenReturn(BigDecimal.ZERO);
        // updateBalance returns 0 -> insufficient balance -> throws before save
        when(walletRepository.updateBalance(senderId, new BigDecimal("-20.00"))).thenReturn(0);

        TransactionService service = newService();
        assertThatThrownBy(() -> service.transfer(
                new TransferRequest(receiverId, new BigDecimal("20.00"), "2468", "lock-order"),
                userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient balance");

        InOrder order = inOrder(walletRepository);
        order.verify(walletRepository).findByIdWithLock(lower);
        order.verify(walletRepository).findByIdWithLock(higher);

        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void lockOrder_senderGreaterThanReceiver_locksLowerFirst() {
        UUID senderId = new UUID(0L, 2L);
        UUID receiverId = new UUID(0L, 1L);
        UUID userId = UUID.randomUUID();
        runDirection(senderId, receiverId, userId);
    }

    @Test
    void lockOrder_senderLessThanReceiver_locksLowerFirst() {
        UUID senderId = new UUID(0L, 1L);
        UUID receiverId = new UUID(0L, 2L);
        UUID userId = UUID.randomUUID();
        runDirection(senderId, receiverId, userId);
    }
}