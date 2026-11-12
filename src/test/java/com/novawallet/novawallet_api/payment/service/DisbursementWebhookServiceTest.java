package com.novawallet.novawallet_api.payment.service;

import com.novawallet.novawallet_api.auth.service.AuthService;
import com.novawallet.novawallet_api.fee.service.FeeEngineService;
import com.novawallet.novawallet_api.payment.PaymentGateway;
import com.novawallet.novawallet_api.payment.dto.GatewayCallbackPayload;
import com.novawallet.novawallet_api.payment.dto.GatewayStatusResponse;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Batch 3 (B3.3): disbursement webhook — durable dedup, reference/amount
 * matching, quarantine, and authoritative provider re-query.
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@TestPropertySource(properties = {
        "app.payments.lipila.enabled=true",
        "app.payments.lipila.webhook-secret=9hXwJfvyJqx7Oslku3FDvn8tRrV3+RZ8achVCXWTbQI="
})
class DisbursementWebhookServiceTest {

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

    @Autowired private WebhookService webhookService;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PaymentGateway paymentGateway;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        reset(paymentGateway);
        int suffix = COUNTER.incrementAndGet();

        User user = User.builder()
                .email("wh-" + suffix + "@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("+260999" + String.format("%05d", suffix))
                .passwordHash("hash")
                .emailVerified(true)
                .build();
        user = userRepository.saveAndFlush(user);
        userId = user.getId();

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .accountNumber("W" + String.format("%06d", suffix))
                .balance(new BigDecimal("1000.00"))
                .currency("ZMW")
                .build();
        wallet = walletRepository.saveAndFlush(wallet);
        walletId = wallet.getId();
    }

    private Transaction seedPayout(String providerReference, BigDecimal amount) {
        int suffix = COUNTER.incrementAndGet();
        Transaction tx = Transaction.builder()
                .reference("NWTXWH" + String.format("%06d", suffix))
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .balanceBefore(new BigDecimal("1100.00"))
                .balanceAfter(new BigDecimal("1000.00"))
                .status(TransactionStatus.PENDING_RECONCILIATION)
                .description("Payout [ACCT:W000001] [LIPILA:" + providerReference + "]")
                .senderWalletId(walletId)
                .providerReference(providerReference)
                .externalStatus("Pending")
                .payoutAccount("260961234567")
                .payoutChannel("mobile-money")
                .attemptCount(0)
                .build();
        tx = transactionRepository.saveAndFlush(tx);
        jdbcTemplate.update("UPDATE transactions SET created_at = ? WHERE id = ?", LocalDateTime.now().minusMinutes(30), tx.getId());
        return tx;
    }

    private String nowTs() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    /** Lipila (Standard Webhooks) signature for the test secret (base64-decoded). */
    private String sign(String eventId, String timestamp, String rawBody) {
        try {
            byte[] key = java.util.Base64.getDecoder()
                    .decode("9hXwJfvyJqx7Oslku3FDvn8tRrV3+RZ8achVCXWTbQI=");
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
            byte[] bytes = mac.doFinal((eventId + "." + timestamp + "." + rawBody).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "v1," + java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GatewayCallbackPayload payload(String reference, BigDecimal amount) {
        return new GatewayCallbackPayload(reference, "Successful", amount, "260961234567",
                "MtnMoney", "LP-1", "ok");
    }

    @Test
    @DisplayName("B3.3: valid disbursement event finalizes the payout via authoritative re-query")
    void disbursementWebhook_successEvent_finalizes() {
        Transaction payout = seedPayout("NW:wh-success", new BigDecimal("100.00"));
        when(paymentGateway.checkStatus("NW:wh-success"))
                .thenReturn(new GatewayStatusResponse("NW:wh-success", "Successful", "Paid"));

        String tsSuccess = nowTs();
        String sigSuccess = sign("evt-success", tsSuccess, "{}");
        WebhookService.DisbursementWebhookResult result = webhookService.processDisbursementCallback(
                payload("NW:wh-success", new BigDecimal("100.00")),
                "evt-success", tsSuccess, sigSuccess, "{}");

        assertThat(result).isEqualTo(WebhookService.DisbursementWebhookResult.PROCESSED);
        verify(paymentGateway, times(1)).checkStatus("NW:wh-success");

        Transaction reloaded = transactionRepository.findById(payout.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.SUCCESSFUL);
    }

    @Test
    @DisplayName("B3.3: duplicate event id is a no-op (no second provider call or financial effect)")
    void disbursementWebhook_duplicateEvent_noSecondEffect() {
        seedPayout("NW:wh-dup", new BigDecimal("100.00"));
        when(paymentGateway.checkStatus("NW:wh-dup"))
                .thenReturn(new GatewayStatusResponse("NW:wh-dup", "Successful", "Paid"));

        String tsDup = nowTs();
        String sigDup = sign("evt-dup", tsDup, "{}");
        WebhookService.DisbursementWebhookResult first = webhookService.processDisbursementCallback(
                payload("NW:wh-dup", new BigDecimal("100.00")), "evt-dup", tsDup, sigDup, "{}");
        WebhookService.DisbursementWebhookResult second = webhookService.processDisbursementCallback(
                payload("NW:wh-dup", new BigDecimal("100.00")), "evt-dup", tsDup, sigDup, "{}");

        assertThat(first).isEqualTo(WebhookService.DisbursementWebhookResult.PROCESSED);
        assertThat(second).isEqualTo(WebhookService.DisbursementWebhookResult.DUPLICATE);
        verify(paymentGateway, times(1)).checkStatus("NW:wh-dup");
    }

    @Test
    @DisplayName("B3.3: amount mismatch is quarantined — no finalization, funds preserved")
    void disbursementWebhook_amountMismatch_quarantined() {
        Transaction payout = seedPayout("NW:wh-mismatch", new BigDecimal("100.00"));

        String tsMismatch = nowTs();
        String sigMismatch = sign("evt-mismatch", tsMismatch, "{}");
        WebhookService.DisbursementWebhookResult result = webhookService.processDisbursementCallback(
                payload("NW:wh-mismatch", new BigDecimal("200.00")), "evt-mismatch", tsMismatch, sigMismatch, "{}");

        assertThat(result).isEqualTo(WebhookService.DisbursementWebhookResult.QUARANTINED);
        verify(paymentGateway, never()).checkStatus(any());

        Transaction reloaded = transactionRepository.findById(payout.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("B3.3: unknown reference is quarantined, never finalized")
    void disbursementWebhook_unknownReference_quarantined() {
        String tsUnknown = nowTs();
        String sigUnknown = sign("evt-unknown", tsUnknown, "{}");
        WebhookService.DisbursementWebhookResult result = webhookService.processDisbursementCallback(
                payload("NW:wh-unknown", new BigDecimal("100.00")), "evt-unknown", tsUnknown, sigUnknown, "{}");

        assertThat(result).isEqualTo(WebhookService.DisbursementWebhookResult.QUARANTINED);
        verify(paymentGateway, never()).checkStatus(any());
    }

    @Test
    @DisplayName("B3.3: stale timestamp is rejected before any processing")
    void disbursementWebhook_staleTimestamp_rejected() {
        String stale = String.valueOf(Instant.now().getEpochSecond() - 600);
        assertThatThrownBy(() -> webhookService.processDisbursementCallback(
                payload("NW:wh-stale", new BigDecimal("100.00")), "evt-stale", stale, null, "{}"))
                .isInstanceOf(com.novawallet.novawallet_api.exception.BadRequestException.class);
        verify(paymentGateway, never()).checkStatus(any());
    }

    @Test
    @DisplayName("B3.3: provider re-query failure defers the event, leaves the payout untouched")
    void disbursementWebhook_providerFailure_deferred() {
        Transaction payout = seedPayout("NW:wh-deferred", new BigDecimal("100.00"));
        when(paymentGateway.checkStatus("NW:wh-deferred"))
                .thenThrow(new RuntimeException("provider unreachable"));

        String tsDeferred = nowTs();
        String sigDeferred = sign("evt-deferred", tsDeferred, "{}");
        WebhookService.DisbursementWebhookResult result = webhookService.processDisbursementCallback(
                payload("NW:wh-deferred", new BigDecimal("100.00")), "evt-deferred", tsDeferred, sigDeferred, "{}");

        assertThat(result).isEqualTo(WebhookService.DisbursementWebhookResult.DEFERRED);
        Transaction reloaded = transactionRepository.findById(payout.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.PENDING_RECONCILIATION);
    }

    @Test
    @DisplayName("F2: the authoritative provider re-query runs outside any database transaction")
    void providerStatusQuery_runsOutsideDatabaseTransaction() {
        seedPayout("NW:wh-notx", new BigDecimal("100.00"));
        when(paymentGateway.checkStatus("NW:wh-notx")).thenAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive())
                    .as("F2: no DB transaction may be active during the provider HTTP call")
                    .isFalse();
            return new GatewayStatusResponse("NW:wh-notx", "Successful", "Paid");
        });

        String ts = nowTs();
        WebhookService.DisbursementWebhookResult result = webhookService.processDisbursementCallback(
                payload("NW:wh-notx", new BigDecimal("100.00")), "evt-notx", ts,
                sign("evt-notx", ts, "{}"), "{}");

        assertThat(result).isEqualTo(WebhookService.DisbursementWebhookResult.PROCESSED);
    }

    @Test
    @DisplayName("F2: an event for an already-terminal payout applies no second state change")
    void alreadyFinalizedPayout_eventProcessed_noStateChange() {
        Transaction payout = seedPayout("NW:wh-terminal", new BigDecimal("100.00"));
        payout.setStatus(TransactionStatus.SUCCESSFUL);
        transactionRepository.saveAndFlush(payout);
        when(paymentGateway.checkStatus("NW:wh-terminal"))
                .thenReturn(new GatewayStatusResponse("NW:wh-terminal", "Successful", "Paid"));

        String ts = nowTs();
        WebhookService.DisbursementWebhookResult result = webhookService.processDisbursementCallback(
                payload("NW:wh-terminal", new BigDecimal("100.00")), "evt-terminal", ts,
                sign("evt-terminal", ts, "{}"), "{}");

        assertThat(result).isEqualTo(WebhookService.DisbursementWebhookResult.PROCESSED);
        Transaction reloaded = transactionRepository.findById(payout.getId()).orElseThrow();
        assertThat(reloaded.getStatus())
                .as("A stale success response must not re-settle a terminal payout")
                .isEqualTo(TransactionStatus.SUCCESSFUL);
    }
}
