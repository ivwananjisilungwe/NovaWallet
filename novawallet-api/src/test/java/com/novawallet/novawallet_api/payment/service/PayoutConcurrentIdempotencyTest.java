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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S5 remediation test: two concurrent direct-service payout() calls carrying the
 * same idempotency key must produce exactly one payout, one debit and one
 * provider call — the loser replays the winner's record. Exercises the
 * user-scoped key guard (wallet pessimistic lock + V14 unique index backstop).
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@TestPropertySource(properties = "app.payments.lipila.enabled=true")
class PayoutConcurrentIdempotencyTest {

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
                .email("concurrent-" + suffix + "@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("+260992" + String.format("%05d", suffix))
                .passwordHash("hash")
                .emailVerified(true)
                .build();
        user = userRepository.save(user);
        userId = user.getId();

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .accountNumber("Q" + String.format("%06d", suffix))
                .balance(new BigDecimal("1000.00"))
                .currency("ZMW")
                .build();
        wallet = walletRepository.save(wallet);
        walletId = wallet.getId();

        when(authService.verifyPin(any(UUID.class), anyString())).thenReturn(true);
        when(feeEngineService.calculateFee(any(), any())).thenReturn(new BigDecimal("1.00"));
    }

    @Test
    @DisplayName("S5: two concurrent payout() calls with the same key -> one payout, one debit, one gateway call")
    void concurrentSameKey_onePayout() throws InterruptedException {
        when(paymentGateway.initiateDisbursement(any()))
                .thenReturn(new GatewayInitiationResponse("NW:ref", "Successful", "Done"));

        CountDownLatch startGate = new CountDownLatch(2);
        AtomicReference<PayoutResponse> first = new AtomicReference<>();
        AtomicReference<PayoutResponse> second = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Runnable call = new Runnable() {
            @Override
            public void run() {
                try {
                    startGate.countDown();
                    startGate.await(5, TimeUnit.SECONDS);
                    PayoutResponse response = payoutService.payout(
                            walletId,
                            new PayoutRequest(new BigDecimal("100.00"), "1234", "260961234567", "concurrent test"),
                            userId,
                            "shared-key");
                    if (first.compareAndSet(null, response)) {
                        return;
                    }
                    second.set(response);
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            }
        };

        Thread t1 = new Thread(call, "payout-1");
        Thread t2 = new Thread(call, "payout-2");
        t1.start();
        t2.start();
        t1.join(30000);
        t2.join(30000);

        assertThat(failure.get())
                .as("Both concurrent callers must resolve (winner + replay), no unhandled failure")
                .isNull();

        List<Transaction> withdrawals = transactionRepository.findAll().stream()
                .filter(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.WITHDRAWAL)
                .toList();
        assertThat(withdrawals)
                .as("Exactly one WITHDRAWAL row must exist for the shared key")
                .hasSize(1);

        verify(paymentGateway, times(1)).initiateDisbursement(any());

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance())
                .as("Wallet debited exactly once (1000 - 100 - 1 fee)")
                .isEqualByComparingTo(new BigDecimal("899.00"));

        assertThat(second.get()).isNotNull();
        assertThat(first.get()).isNotNull();
        assertThat(second.get().transactionReference())
                .as("The loser replays the winner's payout")
                .isEqualTo(first.get().transactionReference());
    }
}
