package com.novawallet.novawallet_api.payment.service;

import com.novawallet.novawallet_api.auth.service.AuthService;
import com.novawallet.novawallet_api.fee.service.FeeEngineService;
import com.novawallet.novawallet_api.payment.PaymentGateway;
import com.novawallet.novawallet_api.payment.dto.PayoutRequest;
import com.novawallet.novawallet_api.payment.dto.PayoutResponse;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.transaction.service.TransactionLimitService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * S7 remediation tests: exceptional execution failures must never leave a
 * durable debit in an indeterminate state — Exception paths become
 * PENDING_RECONCILIATION (recoverable), Error paths propagate to the JVM
 * (never swallowed).
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@TestPropertySource(properties = "app.payments.lipila.enabled=true")
class PayoutErrorPropagationTest {

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
        AuthService mockAuthService() {
            return mock(AuthService.class);
        }

        @Bean
        @Primary
        TransactionLimitService mockTransactionLimitService() {
            return mock(TransactionLimitService.class);
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
    @Autowired private UserRepository userRepository;
    @Autowired private PaymentGateway paymentGateway;
    @Autowired private AuthService authService;
    @Autowired private TransactionLimitService transactionLimitService;
    @Autowired private FeeEngineService feeEngineService;

    private UUID userId;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        reset(paymentGateway, authService, transactionLimitService, feeEngineService);
        userId = UUID.randomUUID();
        int suffix = COUNTER.incrementAndGet();

        User user = User.builder()
                .email("errprop-" + suffix + "@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("+260991" + String.format("%05d", suffix))
                .passwordHash("hash")
                .emailVerified(true)
                .build();
        user = userRepository.save(user);
        userId = user.getId();

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .accountNumber("P" + String.format("%06d", suffix))
                .balance(new BigDecimal("1000.00"))
                .currency("ZMW")
                .build();
        wallet = walletRepository.save(wallet);
        walletId = wallet.getId();

        when(authService.verifyPin(any(UUID.class), anyString())).thenReturn(true);
        when(feeEngineService.calculateFee(any(), any())).thenReturn(new BigDecimal("1.00"));
    }

    private PayoutRequest request() {
        return new PayoutRequest(
                new BigDecimal("100.00"),
                "1234",
                "260961234567",
                "Payout error propagation test"
        );
    }

    @Test
    @DisplayName("S7: JVM Error (InternalError) propagates out of payout(); payout stays PENDING, debit durable")
    void jvmError_propagates_payoutStaysPending() {
        when(paymentGateway.initiateDisbursement(any()))
                .thenThrow(new InternalError("simulated JVM failure"));

        assertThatThrownBy(() -> payoutService.payout(walletId, request(), userId, "key-jvm-error"))
                .as("Errors must never be swallowed by the payout uncertain-path")
                .isInstanceOf(InternalError.class);

        Transaction payout = transactionRepository.findAll().stream()
                .filter(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.WITHDRAWAL)
                .findFirst()
                .orElseThrow();
        assertThat(payout.getStatus())
                .as("Escaping Error must leave the payout PENDING for reconciliation")
                .isEqualTo(TransactionStatus.PENDING);

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance())
                .as("Stage A debit is durable (1000 - 100 - 1 fee)")
                .isEqualByComparingTo(new BigDecimal("899.00"));
    }

    @Test
    @DisplayName("S7: RuntimeException from provider -> PENDING_RECONCILIATION, debit preserved, no compensating credit")
    void runtimeException_uncertain_pendingReconciliation() {
        when(paymentGateway.initiateDisbursement(any()))
                .thenThrow(new RuntimeException("connect timed out"));

        PayoutResponse response = payoutService.payout(walletId, request(), userId, "key-runtime");

        assertThat(response.gatewayStatus()).isEqualTo("Pending");
        assertThat(response.gatewayMessage())
                .as("No provider-internal text may surface to the customer")
                .doesNotContain("timed out");

        Transaction payout = transactionRepository.findAll().stream()
                .filter(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.WITHDRAWAL)
                .findFirst()
                .orElseThrow();
        assertThat(payout.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance())
                .as("Debit preserved for uncertain outcome — never auto-refunded")
                .isEqualByComparingTo(new BigDecimal("899.00"));

        boolean hasDeposit = transactionRepository.findAll().stream()
                .anyMatch(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.DEPOSIT);
        assertThat(hasDeposit)
                .as("No compensating credit for an uncertain outcome")
                .isFalse();
    }
}
