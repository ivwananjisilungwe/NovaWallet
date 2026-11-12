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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
@TestPropertySource(properties = "app.payments.lipila.enabled=true")
class PayoutFinalizationIdempotencyTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @TestConfiguration
    static class MockBeansConfig {
        @Bean @Primary PaymentGateway mockPaymentGateway() { return mock(PaymentGateway.class); }
        @Bean @Primary AuditService mockAuditService() { return mock(AuditService.class); }
        @Bean @Primary FeeEngineService mockFeeEngineService() { return mock(FeeEngineService.class); }
    }

    @Autowired private PayoutService payoutService;
    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private AuditService auditService;
    @Autowired private FeeEngineService feeEngineService;
    @Autowired private PaymentGateway paymentGateway;
    @Autowired private UserRepository userRepository;

    private UUID userId;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        clearInvocations(auditService);
        reset(feeEngineService, paymentGateway);
        userId = UUID.randomUUID();
        int suffix = COUNTER.incrementAndGet();

        User user = User.builder()
                .email("fid-" + suffix + "@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("+260975" + String.format("%05d", suffix))
                .passwordHash("hash")
                .emailVerified(true)
                .build();
        user = userRepository.save(user);
        userId = user.getId();

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .accountNumber("F" + String.format("%06d", suffix))
                .balance(new BigDecimal("1000.00"))
                .currency("ZMW")
                .build();
        wallet = walletRepository.save(wallet);
        walletId = wallet.getId();

        when(feeEngineService.calculateFee(any(), any())).thenReturn(new BigDecimal("1.00"));
    }

    private Transaction seedPendingPayout(BigDecimal amount) {
        Transaction tx = Transaction.builder()
                .reference("NWTXFI" + String.format("%06d", COUNTER.incrementAndGet()))
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1000.00").subtract(amount))
                .status(TransactionStatus.PENDING)
                .description("Payout [ACCT:F000001] [LIPILA:NW:ref-fid]")
                .senderWalletId(walletId)
                .build();
        return transactionRepository.save(tx);
    }

    @Test
    void duplicateFinalisePendingReconciliation_appliesNoSecondCredit() {
        Transaction payout = seedPendingPayout(new BigDecimal("200.00"));
        when(paymentGateway.initiateDisbursement(any()))
                .thenThrow(new RuntimeException("connect timed out"));

        Transaction first = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref-fid", null, "uncertain"), userId);
        assertThat(first.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);

        BigDecimal balanceBefore = walletRepository.findById(walletId).orElseThrow().getBalance();

        clearInvocations(auditService);
        Transaction second = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref-fid", "Failed", "stale"), userId);

        assertThat(second.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        BigDecimal balanceAfter = walletRepository.findById(walletId).orElseThrow().getBalance();
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore);

        long depositCount = transactionRepository.findAll().stream()
                .filter(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.DEPOSIT)
                .count();
        assertThat(depositCount).as("Exactly one compensating DEPOSIT row total").isEqualTo(0);

        verifyNoInteractions(auditService);

        Transaction persisted = transactionRepository.findByReference(payout.getReference()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
    }

    @Test
    void alreadySuccessful_neverFlipsToFailedOnStaleResponse() {
        Transaction payout = seedPendingPayout(new BigDecimal("200.00"));
        when(paymentGateway.initiateDisbursement(any()))
                .thenReturn(new GatewayInitiationResponse("NW:ref-fid", "Successful", "Paid"));

        Transaction first = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref-fid", "Successful", "Paid"), userId);
        assertThat(first.getStatus()).isEqualTo(TransactionStatus.SUCCESSFUL);

        BigDecimal balanceBefore = walletRepository.findById(walletId).orElseThrow().getBalance();

        clearInvocations(auditService);
        Transaction second = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref-fid", "Failed", "stale"), userId);

        assertThat(second.getStatus()).isEqualTo(TransactionStatus.SUCCESSFUL);
        BigDecimal balanceAfter = walletRepository.findById(walletId).orElseThrow().getBalance();
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore);

        long depositCount = transactionRepository.findAll().stream()
                .filter(t -> t.getSenderWalletId().equals(walletId)
                        && t.getType() == TransactionType.DEPOSIT)
                .count();
        assertThat(depositCount).as("No compensating DEPOSIT row").isEqualTo(0);

        verifyNoInteractions(auditService);
    }

    @Test
    void alreadyFailed_neverFlipsBackToSuccessful() {
        Transaction payout = seedPendingPayout(new BigDecimal("200.00"));
        when(paymentGateway.initiateDisbursement(any()))
                .thenReturn(new GatewayInitiationResponse("NW:ref-fid", "Failed", "Insufficient float"));

        Transaction first = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref-fid", "Failed", "Insufficient float"), userId);
        assertThat(first.getStatus()).isEqualTo(TransactionStatus.FAILED);

        BigDecimal balanceAfterFirst = walletRepository.findById(walletId).orElseThrow().getBalance();

        clearInvocations(auditService);
        Transaction second = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref-fid", "Successful", "stale"), userId);

        assertThat(second.getStatus()).isEqualTo(TransactionStatus.FAILED);
        BigDecimal balanceAfterSecond = walletRepository.findById(walletId).orElseThrow().getBalance();
        assertThat(balanceAfterSecond).isEqualByComparingTo(balanceAfterFirst);

        verifyNoInteractions(auditService);
    }

    @Test
    void guardDecidesFromPersistedStatusNotFromCaller() {
        Transaction payout = seedPendingPayout(new BigDecimal("200.00"));
        when(paymentGateway.initiateDisbursement(any()))
                .thenReturn(new GatewayInitiationResponse("NW:ref-fid", "Failed", "Insufficient float"));

        Transaction failed = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref-fid", "Failed", "Insufficient float"), userId);
        assertThat(failed.getStatus()).isEqualTo(TransactionStatus.FAILED);

        Transaction persisted = transactionRepository.findByReference(payout.getReference()).orElseThrow();
        persisted.setStatus(TransactionStatus.PENDING_RECONCILIATION);
        transactionRepository.saveAndFlush(persisted);

        Transaction again = payoutService.finalizePayout(payout.getId(),
                new GatewayInitiationResponse("NW:ref-fid", "Successful", "stale"), userId);
        assertThat(again.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
    }
}
