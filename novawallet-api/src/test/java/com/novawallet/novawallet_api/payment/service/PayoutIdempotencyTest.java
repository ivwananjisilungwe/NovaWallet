package com.novawallet.novawallet_api.payment.service;

import com.novawallet.novawallet_api.auth.service.AuthService;
import com.novawallet.novawallet_api.fee.service.FeeEngineService;
import com.novawallet.novawallet_api.payment.PaymentGateway;
import com.novawallet.novawallet_api.payment.dto.GatewayInitiationResponse;
import com.novawallet.novawallet_api.payment.dto.PayoutRequest;
import com.novawallet.novawallet_api.payment.dto.PayoutResponse;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
@TestPropertySource(properties = "app.payments.lipila.enabled=true")
class PayoutIdempotencyTest {

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
                .email("idemp-" + suffix + "@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("+260972" + String.format("%05d", suffix))
                .passwordHash("hash")
                .emailVerified(true)
                .build();
        user = userRepository.save(user);
        userId = user.getId();

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .accountNumber("I" + String.format("%06d", suffix))
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
                "Payout idempotency test"
        );
    }

    @Test
    @DisplayName("Same key, same request -> one payout (one debit, one gateway call)")
    void sameKeySameRequest_onePayout() {
        when(paymentGateway.initiateDisbursement(any()))
                .thenReturn(new GatewayInitiationResponse("NW:ref", "Successful", "Done"));

        PayoutResponse first = payoutService.payout(walletId, request(), userId, "key1");
        PayoutResponse second = payoutService.payout(walletId, request(), userId, "key1");

        assertThat(second.transactionReference()).isEqualTo(first.transactionReference());
        assertThat(second.amount()).isEqualByComparingTo(first.amount());

        long withdrawals = transactionRepository.findAll().stream()
                .filter(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.WITHDRAWAL)
                .count();
        assertThat(withdrawals).as("Only one WITHDRAWAL row for same idempotency key").isEqualTo(1);

        verify(paymentGateway, times(1)).initiateDisbursement(any());
    }

    @Test
    @DisplayName("Same key, different amount -> original response returned")
    void sameKeyDifferentAmount_returnsOriginalResponse() {
        when(paymentGateway.initiateDisbursement(any()))
                .thenReturn(new GatewayInitiationResponse("NW:ref", "Successful", "Done"));

        PayoutResponse first = payoutService.payout(walletId, request(), userId, "key1");

        PayoutRequest differentAmount = new PayoutRequest(
                new BigDecimal("200.00"), "1234", "260961234567", "Payout idempotency test"
        );
        PayoutResponse second = payoutService.payout(walletId, differentAmount, userId, "key1");

        assertThat(second.transactionReference()).isEqualTo(first.transactionReference());
        assertThat(second.amount()).isEqualByComparingTo(first.amount());
        assertThat(second.amount()).isEqualByComparingTo(new BigDecimal("100.00"));

        long withdrawals = transactionRepository.findAll().stream()
                .filter(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.WITHDRAWAL)
                .count();
        assertThat(withdrawals).as("Only one WITHDRAWAL row despite different amount").isEqualTo(1);
    }

    @Test
    @DisplayName("Different keys -> separate payouts (two debits, two gateway calls)")
    void differentKeys_separatePayouts() {
        when(paymentGateway.initiateDisbursement(any()))
                .thenReturn(new GatewayInitiationResponse("NW:ref", "Successful", "Done"));

        PayoutResponse first = payoutService.payout(walletId, request(), userId, "key1");
        PayoutResponse second = payoutService.payout(walletId, request(), userId, "key2");

        assertThat(second.transactionReference()).isNotEqualTo(first.transactionReference());

        long withdrawals = transactionRepository.findAll().stream()
                .filter(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.WITHDRAWAL)
                .count();
        assertThat(withdrawals).as("Two WITHDRAWAL rows for different keys").isEqualTo(2);

        verify(paymentGateway, times(2)).initiateDisbursement(any());
    }

    @Test
    @DisplayName("No key -> each request processed (two debits)")
    void noKey_eachRequestProcessed() {
        when(paymentGateway.initiateDisbursement(any()))
                .thenReturn(new GatewayInitiationResponse("NW:ref", "Successful", "Done"));

        PayoutResponse first = payoutService.payout(walletId, request(), userId, null);
        PayoutResponse second = payoutService.payout(walletId, request(), userId, null);

        assertThat(first.transactionReference()).isNotEqualTo(second.transactionReference());

        long withdrawals = transactionRepository.findAll().stream()
                .filter(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.WITHDRAWAL)
                .count();
        assertThat(withdrawals).as("Two WITHDRAWAL rows when no idempotency key").isEqualTo(2);
    }
}
