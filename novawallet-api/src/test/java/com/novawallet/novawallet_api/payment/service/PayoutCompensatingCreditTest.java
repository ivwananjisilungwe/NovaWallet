package com.novawallet.novawallet_api.payment.service;

import com.novawallet.novawallet_api.audit.service.AuditService;
import com.novawallet.novawallet_api.fee.service.FeeEngineService;
import com.novawallet.novawallet_api.payment.PaymentGateway;
import com.novawallet.novawallet_api.payment.dto.GatewayInitiationResponse;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
@TestPropertySource(properties = "app.payments.lipila.enabled=true")
class PayoutCompensatingCreditTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @TestConfiguration
    static class MockBeansConfig {
        @Bean
        @Primary
        PaymentGateway mockPaymentGateway() {
            return mock(PaymentGateway.class);
        }

        @Bean
        @Primary
        AuditService mockAuditService() {
            return mock(AuditService.class);
        }

        @Bean
        @Primary
        FeeEngineService mockFeeEngineService() {
            return mock(FeeEngineService.class);
        }
    }

    @Autowired private PayoutService payoutService;
    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private AuditService auditService;
    @Autowired private FeeEngineService feeEngineService;
    @Autowired private UserRepository userRepository;

    private UUID userId;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        reset(auditService, feeEngineService);
        userId = UUID.randomUUID();
        int suffix = COUNTER.incrementAndGet();

        User user = User.builder()
                .email("comp-" + suffix + "@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("+260973" + String.format("%05d", suffix))
                .passwordHash("hash")
                .emailVerified(true)
                .build();
        user = userRepository.save(user);
        userId = user.getId();

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .accountNumber("C" + String.format("%06d", suffix))
                .balance(new BigDecimal("799.00"))
                .currency("ZMW")
                .build();
        wallet = walletRepository.save(wallet);
        walletId = wallet.getId();

        when(feeEngineService.calculateFee(any(), any())).thenReturn(new BigDecimal("1.00"));
    }

    private Transaction seedFailedPayout() {
        Transaction tx = Transaction.builder()
                .reference("NWTXCC" + String.format("%06d", COUNTER.incrementAndGet()))
                .type(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("200.00"))
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("799.00"))
                .status(TransactionStatus.PENDING)
                .description("Payout [ACCT:C000001] [LIPILA:NW:ref-comp]")
                .senderWalletId(walletId)
                .build();
        return transactionRepository.save(tx);
    }

    @Test
    @DisplayName("Provider confirms failure -> compensating credit, FAILED status, DEPOSIT row, PAYOUT_FAILED_COMPENSATED audit")
    void providerFailed_appliesCompensatingCredit() {
        Transaction payout = seedFailedPayout();

        Transaction finalized = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref-comp", "Failed", "Insufficient float"), userId);

        assertThat(finalized.getStatus()).isEqualTo(TransactionStatus.FAILED);

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));

        boolean hasDeposit = transactionRepository.findAll().stream()
                .anyMatch(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.DEPOSIT
                        && t.getAmount().compareTo(new BigDecimal("201.00")) == 0);
        assertThat(hasDeposit).as("Compensating DEPOSIT credit row").isTrue();

        verify(auditService, times(1)).recordAction(
                eq("Wallet"), eq(walletId), eq("PAYOUT_FAILED_COMPENSATED"),
                anyString(), anyString(), eq(userId)
        );
    }

    @Test
    @DisplayName("Duplicate failure -> no double credit")
    void duplicateFailure_noDoubleCredit() {
        Transaction payout = seedFailedPayout();

        payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref-comp", "Failed", "Insufficient float"), userId);
        payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref-comp", "Failed", "Insufficient float"), userId);

        long depositCount = transactionRepository.findAll().stream()
                .filter(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.DEPOSIT)
                .count();
        assertThat(depositCount).as("Only one compensating DEPOSIT row").isEqualTo(1);

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Provider success -> no credit, SUCCESSFUL status")
    void providerSuccess_noCompensatingCredit() {
        Transaction payout = seedFailedPayout();

        Transaction finalized = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref-comp", "Successful", "Paid"), userId);

        assertThat(finalized.getStatus()).isEqualTo(TransactionStatus.SUCCESSFUL);

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("799.00"));

        boolean hasDeposit = transactionRepository.findAll().stream()
                .anyMatch(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.DEPOSIT);
        assertThat(hasDeposit).as("No compensating DEPOSIT row on success").isFalse();
    }
}
