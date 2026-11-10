package com.novawallet.novawallet_api.endpoint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code POST /api/v1/wallets/{walletId}/payouts} in the
 * DEFAULT configuration — i.e. with the Lipila integration disabled
 * ({@code app.payments.lipila.enabled=false}, the shipped default).
 *
 * <p>Verifies the fail-closed contract: payouts must return
 * 503 SERVICE_UNAVAILABLE until an operator enables the feature with real
 * credentials, and the endpoint must never be reachable unauthenticated.</p>
 */
class PayoutEndpointIntegrationTest extends EndpointTestSupport {

    @Test
    @DisplayName("Payout returns 503 SERVICE_UNAVAILABLE while the payment gateway is disabled")
    void payout_whenDisabled_returns503() throws Exception {
        RegisteredUser user = registerEndpointUser("payoff");
        var wallet = createWalletFor(user);

        payout(user, wallet.getId().toString())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value(
                        "Mobile-money payouts are not available — payment gateway is disabled"));
    }

    @Test
    @DisplayName("Payout requires authentication")
    void payout_unauthenticated_returns403() throws Exception {
        RegisteredUser user = registerEndpointUser("payauth");
        var wallet = createWalletFor(user);

        mockMvc.perform(post("/api/v1/wallets/" + wallet.getId() + "/payouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payoutBody()))
                .andExpect(status().isForbidden());
    }

    // ==================== Helpers ====================

    private ResultActions payout(RegisteredUser user, String walletId) throws Exception {
        return mockMvc.perform(post("/api/v1/wallets/" + walletId + "/payouts")
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payoutBody()));
    }

    private static String payoutBody() {
        return """
                {
                  "amount": 200.00,
                  "pin": "1234",
                  "accountNumber": "260961234567",
                  "description": "Payout test"
                }
                """;
    }
}
