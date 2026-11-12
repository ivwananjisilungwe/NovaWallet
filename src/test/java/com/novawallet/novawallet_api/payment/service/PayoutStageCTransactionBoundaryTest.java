package com.novawallet.novawallet_api.payment.service;

import com.novawallet.novawallet_api.payment.PaymentGateway;
import com.novawallet.novawallet_api.payment.dto.GatewayInitiationResponse;
import com.novawallet.novawallet_api.payment.dto.PayoutRequest;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
@TestPropertySource(properties = "app.payments.lipila.enabled=true")
class PayoutStageCTransactionBoundaryTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @TestConfiguration
    static class MockBeansConfig {
        @Bean
        @Primary
        PaymentGateway mockPaymentGateway() {
            return mock(PaymentGateway.class);
        }
    }

    @Autowired private PayoutService payoutService;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private WalletRepository walletRepository;
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
                .email("stagec-" + suffix + "@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("+260974" + String.format("%05d", suffix))
                .passwordHash("hash")
                .emailVerified(true)
                .build();
        user = userRepository.save(user);
        userId = user.getId();

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .accountNumber("S" + String.format("%06d", suffix))
                .balance(new BigDecimal("799.50"))
                .currency("ZMW")
                .build();
        wallet = walletRepository.save(wallet);
        walletId = wallet.getId();
    }

    private Transaction seedPayout() {
        Transaction tx = Transaction.builder()
                .reference("NWTXSC" + String.format("%06d", COUNTER.incrementAndGet()))
                .type(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("200.00"))
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("799.50"))
                .status(TransactionStatus.PENDING)
                .description("Payout [ACCT:S000001] [LIPILA:NW:ref-boundary]")
                .senderWalletId(walletId)
                .build();
        return transactionRepository.save(tx);
    }

    // ==================== Stage C: pessimistic lock ====================

    @Test
    @DisplayName("Stage C finalizePayout uses findByIdWithLock (pessimistic lock), not findById")
    void stageC_usesPessimisticLock() throws Exception {
        Transaction payout = seedPayout();

        payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref-boundary", "Successful", "Paid"), userId);

        // Verify finalizePayout succeeded (pessimistic lock acquired and released)
        Transaction reloaded = transactionRepository.findByReference(payout.getReference()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.SUCCESSFUL);

        // Verify via reflection that findByIdWithLock carries @Lock(PESSIMISTIC_WRITE)
        Method findByIdWithLock = WalletRepository.class.getMethod("findByIdWithLock", UUID.class);
        Lock lockAnno = findByIdWithLock.getAnnotation(Lock.class);
        assertThat(lockAnno).as("findByIdWithLock must be @Lock annotated").isNotNull();
        assertThat(lockAnno.value()).as("findByIdWithLock must use PESSIMISTIC_WRITE").isEqualTo(LockModeType.PESSIMISTIC_WRITE);

        // Verify findById (the plain JpaRepository method) is NOT pessimistic-locked
        Method findById = WalletRepository.class.getMethod("findById", Object.class);
        Lock findByIdAnno = findById.getAnnotation(Lock.class);
        assertThat(findByIdAnno).as("findById must NOT be @Lock annotated").isNull();
    }

    // ==================== Stage B: no transaction ====================

    @Test
    @DisplayName("Stage B executeDisbursement calls the gateway directly and passes the response through")
    void stageB_callsGatewayDirectly_passesResponseThrough() {
        when(paymentGateway.initiateDisbursement(any()))
                .thenReturn(new GatewayInitiationResponse("NW:ref-boundary", "Successful", "Paid"));

        GatewayInitiationResponse response = payoutService.executeDisbursement("NW:ref-boundary",
                new PayoutRequest(new BigDecimal("200.00"), "1234", "260961234567", "test"));

        assertThat(response.status()).isEqualTo("Successful");
        assertThat(response.referenceId()).isEqualTo("NW:ref-boundary");
    }

    @Test
    @DisplayName("Stage B executeDisbursement lets gateway exceptions propagate (no transactional rollback wrapper)")
    void stageB_gatewayExceptionPropagates_noTransactionWrapper() {
        when(paymentGateway.initiateDisbursement(any()))
                .thenThrow(new RuntimeException("gateway down"));

        assertThatThrownBy(() -> payoutService.executeDisbursement("NW:ref-boundary",
                new PayoutRequest(new BigDecimal("200.00"), "1234", "260961234567", "test")))
                .isInstanceOf(com.novawallet.novawallet_api.exception.BadRequestException.class)
                .hasMessageContaining("Payout could not be submitted");
    }
}
