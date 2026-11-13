package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Lipila collection webhook
 * ({@code POST /api/v1/webhooks/lipila/collections}).
 *
 * <p>Covers: shared-secret authentication (constant-time verified, 401 on
 * mismatch), containment of Successful callbacks (no credit), and the
 * deliberate 404 for well-formed references pointing at unknown wallets.</p>
 *
 * <p>The webhook route is permitAll in SecurityConfig — these requests carry
 * no Authorization header, which also proves that rule works.</p>
 */
@TestPropertySource(properties = {
        "app.payments.lipila.webhook-secret=test-webhook-secret"
})
class PaymentWebhookEndpointIntegrationTest extends EndpointTestSupport {

    private static final String WEBHOOK_URL = "/api/v1/webhooks/lipila/collections";
    private static final String SECRET = "test-webhook-secret";

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Successful callback with valid secret is contained and does not credit")
    void successfulCallback_withValidSecret_isContained() throws Exception {
        RegisteredUser user = registerEndpointUser("whcredit");
        Wallet wallet = createWalletFor(user);

        String referenceId = referenceFor(wallet);
        postCallback(SECRET, callbackBody(referenceId, "Successful", "150.00"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));

        Wallet reloaded = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        List<Transaction> rows = transactionRepository.findAll().stream()
                .filter(tx -> wallet.getId().equals(tx.getReceiverWalletId())
                        || wallet.getId().equals(tx.getSenderWalletId()))
                .toList();
        assertThat(rows).isEmpty();
    }

    @Test
    @DisplayName("Successful duplicate replay is contained both times")
    void successfulCallback_duplicateReplay_isContainedBothTimes() throws Exception {
        RegisteredUser user = registerEndpointUser("whdup2");
        Wallet wallet = createWalletFor(user);

        String referenceId = referenceFor(wallet);
        String body = callbackBody(referenceId, "Successful", "80.00");

        postCallback(SECRET, body).andExpect(status().isServiceUnavailable());
        postCallback(SECRET, body).andExpect(status().isServiceUnavailable());

        Wallet reloaded = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Successful callback with an arbitrary large amount is contained")
    void successfulCallback_withLargeAmount_isContained() throws Exception {
        RegisteredUser user = registerEndpointUser("whlarge");
        Wallet wallet = createWalletFor(user);

        String referenceId = referenceFor(wallet);
        postCallback(SECRET, callbackBody(referenceId, "Successful", "1000000.00"))
                .andExpect(status().isServiceUnavailable());

        Wallet reloaded = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Successful callback with amount zero is rejected as bad request")
    void successfulCallback_withZeroAmount_returnsBadRequest() throws Exception {
        RegisteredUser user = registerEndpointUser("whzero");
        Wallet wallet = createWalletFor(user);

        String referenceId = referenceFor(wallet);
        postCallback(SECRET, callbackBody(referenceId, "Successful", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        Wallet reloaded = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Successful callback with negative amount is rejected as bad request")
    void successfulCallback_withNegativeAmount_returnsBadRequest() throws Exception {
        RegisteredUser user = registerEndpointUser("whneg");
        Wallet wallet = createWalletFor(user);

        String referenceId = referenceFor(wallet);
        postCallback(SECRET, callbackBody(referenceId, "Successful", "-10.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        Wallet reloaded = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Successful callback for an unknown wallet returns 404")
    void successfulCallback_forUnknownWallet_returnsNotFound() throws Exception {
        UUID unknownWalletId = UUID.randomUUID();
        String referenceId = referenceFor(unknownWalletId);

        postCallback(SECRET, callbackBody(referenceId, "Successful", "25.00"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Failed callback is acknowledged without crediting")
    void failedCallback_acknowledgedWithoutCredit() throws Exception {
        RegisteredUser user = registerEndpointUser("whfail");
        Wallet wallet = createWalletFor(user);

        postCallback(SECRET, callbackBody(referenceFor(wallet), "Failed", "60.00"))
                .andExpect(status().isOk());

        assertThat(walletRepository.findById(wallet.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Malformed reference returns 400")
    void malformedReference_returnsBadRequest() throws Exception {
        String body = callbackBody("not-a-valid-reference", "Successful", "10.00");
        postCallback(SECRET, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Duplicate callback is contained both times")
    void duplicateCallback_isContained_idempotently() throws Exception {
        RegisteredUser user = registerEndpointUser("whdup");
        Wallet wallet = createWalletFor(user);

        String body = callbackBody(referenceFor(wallet), "Successful", "80.00");

        postCallback(SECRET, body).andExpect(status().isServiceUnavailable());
        postCallback(SECRET, body).andExpect(status().isServiceUnavailable()); // replay

        Wallet reloaded = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(reloaded.getBalance())
                .as("contained callback must not credit")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Callback with an invalid secret is rejected with 401")
    void invalidSecret_returns401() throws Exception {
        RegisteredUser user = registerEndpointUser("whbad");
        Wallet wallet = createWalletFor(user);

        postCallback("wrong-secret", callbackBody(referenceFor(wallet), "Successful", "50.00"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        assertThat(walletRepository.findById(wallet.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Callback without any secret header is rejected with 401")
    void missingSecret_returns401() throws Exception {
        RegisteredUser user = registerEndpointUser("whnone");
        Wallet wallet = createWalletFor(user);

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody(referenceFor(wallet), "Successful", "50.00")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Well-formed reference for an unknown wallet returns 404 so misconfigurations surface")
    void unknownWallet_returns404() throws Exception {
        UUID unknownWalletId = UUID.randomUUID();

        postCallback(SECRET, callbackBody(referenceFor(unknownWalletId), "Successful", "25.00"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Pending callback is acknowledged without crediting")
    void pendingCallback_acknowledgedWithoutCredit() throws Exception {
        RegisteredUser user = registerEndpointUser("whpend");
        Wallet wallet = createWalletFor(user);

        postCallback(SECRET, callbackBody(referenceFor(wallet), "Pending", "60.00"))
                .andExpect(status().isOk());

        assertThat(walletRepository.findById(wallet.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== Helpers ====================

    /** Merchant reference convention: NW:&lt;walletId&gt;:&lt;uuid&gt;. */
    private static String referenceFor(Wallet wallet) {
        return referenceFor(wallet.getId());
    }

    private static String referenceFor(UUID walletId) {
        return "NW:" + walletId + ":" + UUID.randomUUID();
    }

    private static String callbackBody(String referenceId, String status, String amount) {
        return """
                {
                  "referenceId": "%s",
                  "status": "%s",
                  "amount": %s,
                  "accountNumber": "260961234567",
                  "paymentType": "MtnMoney",
                  "identifier": "LP-%s",
                  "message": "%s collection"
                }
                """.formatted(referenceId, status, amount, UUID.randomUUID(), status);
    }

    private ResultActions postCallback(String secret, String body) throws Exception {
        return mockMvc.perform(post(WEBHOOK_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Webhook-Secret", secret)
                .content(body));
    }
}
