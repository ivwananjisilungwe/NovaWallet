package com.novawallet.novawallet_api.transaction.controller;

import com.novawallet.novawallet_api.fee.enums.FeeType;
import com.novawallet.novawallet_api.fee.service.FeeEngineService;
import com.novawallet.novawallet_api.notification.service.NotificationService;
import com.novawallet.novawallet_api.transaction.dto.TransferRequest;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.transaction.service.TransactionService;
import com.novawallet.novawallet_api.user.entity.Role;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
class TransferRollbackIntegrationTest {

    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionService transactionService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private FeeEngineService feeEngineService;

    @MockBean
    private NotificationService notificationService;

    @Test
    void transfer_rollsBackWhenNotificationFails() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        User sender = userRepository.save(User.builder()
                .email("rb-sender-" + UUID.randomUUID() + "@test.com")
                .passwordHash("encoded").firstName("RB").lastName("Sender")
                .phone("+26097" + (System.currentTimeMillis() % 100000))
                .role(Role.USER).emailVerified(true).pinHash(passwordEncoder.encode("2468")).build());
        User receiver = userRepository.save(User.builder()
                .email("rb-receiver-" + UUID.randomUUID() + "@test.com")
                .passwordHash("encoded").firstName("RB").lastName("Receiver")
                .phone("+26098" + (System.currentTimeMillis() % 100000))
                .role(Role.USER).emailVerified(true).build());

        Wallet senderWallet = walletRepository.save(Wallet.builder()
                .userId(sender.getId())
                .accountNumber("RB-S-" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .balance(new BigDecimal("100.00")).currency("ZMW").build());
        Wallet receiverWallet = walletRepository.save(Wallet.builder()
                .userId(receiver.getId())
                .accountNumber("RB-R-" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .balance(BigDecimal.ZERO.setScale(2)).currency("ZMW").build());

        // fund already set via builder

        doThrow(new RuntimeException("notification boom"))
                .when(notificationService).sendBoth(any(), any(), any(), any(), any(), any(), any());

        long beforeCount = transactionRepository.findAll().stream()
                .filter(t -> senderWallet.getId().equals(t.getSenderWalletId())
                        || senderWallet.getId().equals(t.getReceiverWalletId())
                        || receiverWallet.getId().equals(t.getSenderWalletId())
                        || receiverWallet.getId().equals(t.getReceiverWalletId()))
                .count();

        assertThatThrownBy(() ->
                transactionService.transfer(
                        new TransferRequest(receiverWallet.getId(), new BigDecimal("20.00"), "2468", "rollback"),
                        sender.getId()))
                .isInstanceOf(RuntimeException.class);

        Wallet sw = walletRepository.findById(senderWallet.getId()).orElseThrow();
        Wallet rw = walletRepository.findById(receiverWallet.getId()).orElseThrow();
        assertThat(sw.getBalance()).isEqualByComparingTo("100.00");
        assertThat(rw.getBalance()).isEqualByComparingTo("0.00");

        long afterCount = transactionRepository.findAll().stream()
                .filter(t -> senderWallet.getId().equals(t.getSenderWalletId())
                        || senderWallet.getId().equals(t.getReceiverWalletId())
                        || receiverWallet.getId().equals(t.getSenderWalletId())
                        || receiverWallet.getId().equals(t.getReceiverWalletId()))
                .count();
        assertThat(afterCount).isEqualTo(beforeCount);
    }
}