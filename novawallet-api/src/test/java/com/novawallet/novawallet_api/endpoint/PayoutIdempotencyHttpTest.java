package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.payment.PaymentGateway;
import com.novawallet.novawallet_api.payment.dto.GatewayInitiationResponse;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level idempotency tests for the payout endpoint (R-05).
 *
 * <p>These exercise the real servlet stack (controller + {@code IdempotencyFilter}
 * + {@code PayoutService} + database) rather than the service layer alone, so
 * that the filter's scoped-key construction and cached-response replay are
 * verified end to end. All Lipila calls remain mocked.</p>
 */
@TestPropertySource(properties = {
        "app.payments.lipila.enabled=true"
})
class PayoutIdempotencyHttpTest extends EndpointTestSupport {

    private static final String PIN = "1234";
    private static final String KEY = "http-idem-key-1";

    @TestConfiguration
    static class MockGatewayConfig {
        @Bean
        @Primary
        PaymentGateway mockPaymentGateway() {
            return Mockito.mock(PaymentGateway.class);
        }
    }

    @Autowired
    private PaymentGateway paymentGateway;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void resetGatewayStub() {
        Mockito.reset(paymentGateway);
        when(paymentGateway.initiateDisbursement(any())).thenReturn(
                new GatewayInitiationResponse("NW:ignored:ref", "Successful", "Disbursement successful"));
    }

    private Wallet fundedWallet(RegisteredUser user, String balance) {
        Wallet w = createWalletFor(user);
        w.setBalance(new BigDecimal(balance));
        return walletRepository.save(w);
    }

    private void setPin(RegisteredUser user) throws Exception {
        mockMvc.perform(post("/api/v1/pin")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"" + PIN + "\"}"))
                .andExpect(status().isOk());
    }

    private ResultActions payout(RegisteredUser user, Wallet wallet, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/wallets/" + wallet.getId() + "/payouts")
                .header("Authorization", "Bearer " + user.token())
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "amount": 200.00,
                          "pin": "%s",
                          "accountNumber": "260961234567",
                          "description": "HTTP idempotency test"
                        }
                        """.formatted(PIN)));
    }

    @Test
    @DisplayName("Repeated identical request with same key produces one debit and a stable replay")
    void repeatedRequest_oneDebit_stableReplay() throws Exception {
        RegisteredUser user = registerEndpointUser("httpidem");
        verifyEmailFor(user);
        Wallet wallet = fundedWallet(user, "500.00");
        setPin(user);

        payout(user, wallet, KEY).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.gatewayStatus").value("Successful"));

        // Replay must not re-debit or re-call the gateway.
        payout(user, wallet, KEY).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.gatewayStatus").value("Successful"));

        // Balance debited exactly once (amount 200 + fee 0.50 = 200.50).
        assertThat(walletRepository.findById(wallet.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("299.50"));

        // Gateway called exactly once across both requests.
        org.mockito.Mockito.verify(paymentGateway, org.mockito.Mockito.times(1))
                .initiateDisbursement(org.mockito.ArgumentMatchers.any());

        // Exactly one WITHDRAWAL row persisted.
        long withdrawals = transactionRepository.findAll().stream()
                .filter(tx -> wallet.getId().equals(tx.getSenderWalletId()))
                .filter(tx -> tx.getType() == TransactionType.WITHDRAWAL)
                .count();
        assertThat(withdrawals).isEqualTo(1);
    }

    @Test
    @DisplayName("Same key reused with a different payload returns the original payout (no second debit)")
    void sameKeyDifferentPayload_returnsOriginal() throws Exception {
        RegisteredUser user = registerEndpointUser("httpidem2");
        verifyEmailFor(user);
        Wallet wallet = fundedWallet(user, "500.00");
        setPin(user);

        // First request: amount 200.
        mockMvc.perform(post("/api/v1/wallets/" + wallet.getId() + "/payouts")
                        .header("Authorization", "Bearer " + user.token())
                        .header("Idempotency-Key", KEY + "-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 200.00,
                                  "pin": "%s",
                                  "accountNumber": "260961234567",
                                  "description": "first"
                                }
                                """.formatted(PIN)))
                .andExpect(status().isCreated());

        // Second request with the SAME key but a different amount must NOT create a second payout.
        mockMvc.perform(post("/api/v1/wallets/" + wallet.getId() + "/payouts")
                        .header("Authorization", "Bearer " + user.token())
                        .header("Idempotency-Key", KEY + "-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 50.00,
                                  "pin": "%s",
                                  "accountNumber": "260961234567",
                                  "description": "second-different-amount"
                                }
                                """.formatted(PIN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(200.00));

        // Balance debited only once for the original 200.50.
        assertThat(walletRepository.findById(wallet.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("299.50"));
    }
}