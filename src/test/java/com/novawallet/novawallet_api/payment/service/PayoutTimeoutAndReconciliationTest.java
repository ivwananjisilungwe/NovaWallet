package com.novawallet.novawallet_api.payment.service;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
@TestPropertySource(properties = "app.payments.lipila.enabled=true")
class PayoutTimeoutAndReconciliationTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @TestConfiguration
    static class MockGatewayConfig {
        @Bean
        @Primary
        PaymentGateway mockPaymentGateway() {
            return mock(PaymentGateway.class);
        }
    }

    @Autowired private PayoutService payoutService;
    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private PaymentGateway paymentGateway;
    @Autowired private UserRepository userRepository;

    private UUID userId;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        reset(paymentGateway);
        userId = UUID.randomUUID();
        int suffix = COUNTER.incrementAndGet();

        User user = User.builder()
                .email("timeout-" + suffix + "@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("+260971" + String.format("%05d", suffix))
                .passwordHash("hash")
                .emailVerified(true)
                .build();
        user = userRepository.save(user);
        userId = user.getId();

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .accountNumber("T" + String.format("%06d", suffix))
                .balance(new BigDecimal("799.50"))
                .currency("ZMW")
                .build();
        wallet = walletRepository.save(wallet);
        walletId = wallet.getId();
    }

    private Transaction seedPayout(String idempotencyKey) {
        Transaction tx = Transaction.builder()
                .reference("NWTXTM" + String.format("%06d", COUNTER.incrementAndGet()))
                .type(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("200.00"))
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("799.50"))
                .status(TransactionStatus.PENDING)
                .description("Payout [ACCT:T000001] [LIPILA:NW:ref] [IDEMPOTENCY:" + idempotencyKey + "]")
                .senderWalletId(walletId)
                .build();
        return transactionRepository.save(tx);
    }

    @Test
    @DisplayName("Provider timeout (RuntimeException) -> PENDING_RECONCILIATION, debit preserved")
    void providerTimeout_marksPendingReconciliation_preservesDebit() {
        Transaction payout = seedPayout("key-timeout");
        when(paymentGateway.initiateDisbursement(any()))
                .thenThrow(new RuntimeException("connect timed out"));

        Transaction finalized = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref", null, "uncertain"), userId);

        assertThat(finalized.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("799.50"));
        assertThat(transactionRepository.findByReference(payout.getReference()))
                .map(Transaction::getType)
                .contains(TransactionType.WITHDRAWAL);
    }

    @Test
    @DisplayName("Provider returns Pending -> PENDING_RECONCILIATION, debit preserved")
    void providerPending_marksPendingReconciliation_preservesDebit() {
        Transaction payout = seedPayout("key-pending");
        when(paymentGateway.initiateDisbursement(any()))
                .thenReturn(new GatewayInitiationResponse("NW:ref", "Pending", "Accepted"));

        Transaction finalized = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref", "Pending", "Accepted"), userId);

        assertThat(finalized.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("799.50"));
    }

    @Test
    @DisplayName("Provider returns null status -> PENDING_RECONCILIATION, debit preserved")
    void providerNullStatus_marksPendingReconciliation_preservesDebit() {
        Transaction payout = seedPayout("key-null");
        when(paymentGateway.initiateDisbursement(any()))
                .thenReturn(new GatewayInitiationResponse("NW:ref", null, null));

        Transaction finalized = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref", null, null), userId);

        assertThat(finalized.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("799.50"));
    }

    @Test
    @DisplayName("Provider returns unknown status (Processing) -> PENDING_RECONCILIATION, debit preserved")
    void providerUnknownStatus_marksPendingReconciliation_preservesDebit() {
        Transaction payout = seedPayout("key-unknown");
        when(paymentGateway.initiateDisbursement(any()))
                .thenReturn(new GatewayInitiationResponse("NW:ref", "Processing", "Processing"));

        Transaction finalized = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref", "Processing", "Processing"), userId);

        assertThat(finalized.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("799.50"));
    }

    @Test
    @DisplayName("Pending reconciliation marks no compensating credit (no DEPOSIT row)")
    void pendingReconciliation_noCompensatingCredit() {
        Transaction payout = seedPayout("key-nocredit");
        when(paymentGateway.initiateDisbursement(any()))
                .thenReturn(new GatewayInitiationResponse("NW:ref", "Pending", "Accepted"));

        payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref", "Pending", "Accepted"), userId);

        boolean hasDeposit = transactionRepository.findAll().stream()
                .anyMatch(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.DEPOSIT);
        assertThat(hasDeposit).as("No compensating DEPOSIT row for uncertain status").isFalse();
    }
}
