package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.wallet.entity.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionEdgeCaseEndpointIntegrationTest extends EndpointTestSupport {

    @Test
    @DisplayName("Deposit validation rejects missing amount")
    void deposit_missingAmount_shouldReturnValidationError() throws Exception {
        RegisteredUser user = registerEndpointUser("missingamount");
        Wallet wallet = createWalletFor(user);

        mockMvc.perform(post("/v1/wallets/" + wallet.getId() + "/deposit")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Missing amount\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Deposit rejects invalid wallet UUID path parameters")
    void deposit_invalidWalletId_shouldReturnBadRequest() throws Exception {
        RegisteredUser user = registerEndpointUser("baduuid");

        mockMvc.perform(post("/v1/wallets/not-a-uuid/deposit")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00,\"description\":\"Bad UUID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Withdrawals are blocked for frozen wallets")
    void withdraw_frozenWallet_shouldReturnBadRequest() throws Exception {
        RegisteredUser user = registerEndpointUser("frozenwithdraw");
        Wallet wallet = createWalletFor(user);
        String adminToken = adminToken();

        mockMvc.perform(post("/v1/admin/wallets/" + wallet.getId() + "/freeze")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"ADMIN_ACTION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FROZEN"));

        mockMvc.perform(post("/v1/wallets/" + wallet.getId() + "/withdraw")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"pin\":\"1234\",\"description\":\"Frozen withdraw\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Wallet is not active"));
    }

    @Test
    @DisplayName("Transfers are blocked for frozen sender wallets")
    void transfer_frozenWallet_shouldReturnBadRequest() throws Exception {
        RegisteredUser sender = registerEndpointUser("frozensender");
        RegisteredUser receiver = registerEndpointUser("receiver");
        Wallet senderWallet = createWalletFor(sender);
        Wallet receiverWallet = createWalletFor(receiver);
        String adminToken = adminToken();

        mockMvc.perform(post("/v1/admin/wallets/" + senderWallet.getId() + "/freeze")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"ADMIN_ACTION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FROZEN"));

        String transferJson = """
                {
                    "receiverWalletId": "%s",
                    "amount": 15.00,
                    "pin": "1234",
                    "description": "Frozen transfer"
                }
                """.formatted(receiverWallet.getId());

        mockMvc.perform(post("/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Sender wallet is not active"));
    }
}
