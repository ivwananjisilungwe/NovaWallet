package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.payment.PaymentGateway;
import com.novawallet.novawallet_api.payment.dto.GatewayInitiationResponse;
import com.novawallet.novawallet_api.payment.dto.GatewayPaymentRequest;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
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
 * Integration tests for the payout happy path with the Lipila integration
 * ENABLED. The {@link PaymentGateway} is replaced with a Mockito mock so no
 * outbound HTTP occurs; assertions cover the guardrail stack (PIN, verified
 * email), the ledger debit (amount + seeded withdrawal fee of 0.5%, min 0.50),
 * the referenceId convention passed to the gateway, and rollback when the
 * provider rejects the disbursement.
 */
@TestPropertySource(properties = {
        "app.payments.lipila.enabled=true"
})
class PayoutEnabledIntegrationTest extends EndpointTestSupport {

    private static final String PIN = "1234";

    /** Replaces the real LipilaGateway bean for this test context only. */
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

    @BeforeEach
    void resetGatewayStub() {
        Mockito.reset(paymentGateway);
    }

    @Test
    @DisplayName("Payout debits wallet (amount + fee) and initiates a disbursement")
    void payout_successful_debitsWalletAndCallsGateway() throws Exception {
        RegisteredUser user = registerEndpointUser("payok");
        verifyEmailFor(user);
        Wallet wallet = fundedWallet(user, "500.00");
        setPin(user);

        when(paymentGateway.initiateDisbursement(any())).thenReturn(
                new GatewayInitiationResponse("NW:ignored:ref", "Successful", "Disbursement successful"));

        payout(user, wallet).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.gatewayStatus").value("Successful"))
                .andExpect(jsonPath("$.data.amount").value(200.00))
                .andExpect(jsonPath("$.data.accountNumber").value("260961234567"));

        // Seeded WITHDRAWAL fee config: percentageFee stored as percent-of-100,
        // so 0.0050% of 200.00 = 0.01 → clamped to minFee 0.50 → total debit 200.50
        assertThat(walletRepository.findById(wallet.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("299.50"));

        ArgumentCaptor<GatewayPaymentRequest> captor =
                ArgumentCaptor.forClass(GatewayPaymentRequest.class);
        verify(paymentGateway).initiateDisbursement(captor.capture());
        GatewayPaymentRequest sent = captor.getValue();
        assertThat(sent.referenceId()).startsWith("NW:" + wallet.getId() + ":");
        assertThat(sent.currency()).isEqualTo("ZMW");
        assertThat(sent.accountNumber()).isEqualTo("260961234567");
        assertThat(sent.amount()).isEqualByComparingTo(new BigDecimal("200.00"));

        boolean hasWithdrawalRow = transactionRowsFor(wallet).stream()
                .anyMatch(tx -> tx.getType() == TransactionType.WITHDRAWAL);
        boolean hasFeeRow = transactionRowsFor(wallet).stream()
                .anyMatch(tx -> tx.getType() == TransactionType.FEE);
        assertThat(hasWithdrawalRow).as("WITHDRAWAL ledger row").isTrue();
        assertThat(hasFeeRow).as("FEE ledger row").isTrue();
    }

    @Test
    @DisplayName("Provider rejection rolls back the local debit and returns 400")
    void payout_providerRejected_rollsBackDebit() throws Exception {
        RegisteredUser user = registerEndpointUser("payrej");
        verifyEmailFor(user);
        Wallet wallet = fundedWallet(user, "500.00");
        setPin(user);

        when(paymentGateway.initiateDisbursement(any())).thenReturn(
                new GatewayInitiationResponse("NW:ignored:ref", "Failed", "Insufficient merchant float"));

        payout(user, wallet)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        assertThat(walletRepository.findById(wallet.getId()).orElseThrow().getBalance())
                .as("rejected payout must not leave a partial debit")
                .isEqualByComparingTo(new BigDecimal("500.00"));
    }

    // ==================== Helpers ====================

    private Wallet fundedWallet(RegisteredUser user, String balance) {
        Wallet wallet = createWalletFor(user);
        wallet.setBalance(new BigDecimal(balance));
        return walletRepository.save(wallet);
    }

    private void setPin(RegisteredUser user) throws Exception {
        mockMvc.perform(post("/api/v1/pin")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"" + PIN + "\"}"))
                .andExpect(status().isOk());
    }

    private ResultActions payout(RegisteredUser user, Wallet wallet) throws Exception {
        return mockMvc.perform(post("/api/v1/wallets/" + wallet.getId() + "/payouts")
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "amount": 200.00,
                          "pin": "%s",
                          "accountNumber": "260961234567",
                          "description": "Payout integration test"
                        }
                        """.formatted(PIN)));
    }

    private List<Transaction> transactionRowsFor(Wallet wallet) {
        return transactionRepository.findAll().stream()
                .filter(tx -> wallet.getId().equals(tx.getSenderWalletId()))
                .toList();
    }

    @Autowired
    private TransactionRepository transactionRepository;
}
