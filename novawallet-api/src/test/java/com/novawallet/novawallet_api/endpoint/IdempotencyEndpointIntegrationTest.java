package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.wallet.entity.Wallet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdempotencyEndpointIntegrationTest extends EndpointTestSupport {

    private void setPin(String token) throws Exception {
        mockMvc.perform(post("/v1/pin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"2468\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("I1: Deposit with Idempotency-Key returns 201 and caches the response")
    void deposit_withIdempotencyKey_shouldSucceed() throws Exception {
        RegisteredUser user = registerEndpointUser("idemdep");
        Wallet wallet = createWalletFor(user);
        String key = "idemp-deposit-1";

        String requestBody = """
                {"amount": 50.00, "description": "Idempotent deposit"}
                """;

        String firstResponse = mockMvc.perform(post("/v1/wallets/{walletId}/deposit", wallet.getId())
                        .header("Authorization", "Bearer " + user.token())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.amount").value(50.00))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Second call – same key, should return the identical cached response
        String secondResponse = mockMvc.perform(post("/v1/wallets/{walletId}/deposit", wallet.getId())
                        .header("Authorization", "Bearer " + user.token())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assertions.assertEquals(firstResponse, secondResponse);
    }

    @Test
    @DisplayName("I2: Different Idempotency-Key produces a different transaction")
    void deposit_withDifferentKey_shouldCreateNewTransaction() throws Exception {
        RegisteredUser user = registerEndpointUser("idemkey2");
        Wallet wallet = createWalletFor(user);

        String requestBody = """
                {"amount": 30.00, "description": "Second idempotent deposit"}
                """;

        String response1 = mockMvc.perform(post("/v1/wallets/{walletId}/deposit", wallet.getId())
                        .header("Authorization", "Bearer " + user.token())
                        .header("Idempotency-Key", "key-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String response2 = mockMvc.perform(post("/v1/wallets/{walletId}/deposit", wallet.getId())
                        .header("Authorization", "Bearer " + user.token())
                        .header("Idempotency-Key", "key-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assertions.assertNotEquals(response1, response2);
    }

    @Test
    @DisplayName("I3: POST without Idempotency-Key behaves normally")
    void deposit_withoutKey_shouldBehaveNormally() throws Exception {
        RegisteredUser user = registerEndpointUser("idemnokey");
        Wallet wallet = createWalletFor(user);

        String requestBody = """
                {"amount": 10.00, "description": "No key deposit"}
                """;

        mockMvc.perform(post("/v1/wallets/{walletId}/deposit", wallet.getId())
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // A second identical request without a key should also succeed
        mockMvc.perform(post("/v1/wallets/{walletId}/deposit", wallet.getId())
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("I4: GET request ignores Idempotency-Key header")
    void getRequest_ignoresIdempotencyKey() throws Exception {
        RegisteredUser user = registerEndpointUser("idemget");
        Wallet wallet = createWalletFor(user);

        mockMvc.perform(get("/v1/wallets/{walletId}/balance", wallet.getId())
                        .header("Authorization", "Bearer " + user.token())
                        .header("Idempotency-Key", "should-be-ignored"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("I5: Withdrawal with Idempotency-Key caches and replays")
    void withdraw_withIdempotencyKey_shouldSucceed() throws Exception {
        RegisteredUser user = registerEndpointUser("idemwith");
        Wallet wallet = createWalletFor(user);

        // Set PIN before financial operations
        setPin(user.token());

        // Fund first
        mockMvc.perform(post("/v1/wallets/{walletId}/deposit", wallet.getId())
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 200.00, \"description\": \"Fund for withdrawal\"}"))
                .andExpect(status().isCreated());

        String key = "idemp-withdraw-1";
        String requestBody = """
                {"amount": 50.00, "pin": "2468", "description": "Idempotent withdrawal"}
                """;

        String firstResponse = mockMvc.perform(post("/v1/wallets/{walletId}/withdraw", wallet.getId())
                        .header("Authorization", "Bearer " + user.token())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(post("/v1/wallets/{walletId}/withdraw", wallet.getId())
                        .header("Authorization", "Bearer " + user.token())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assertions.assertEquals(firstResponse, secondResponse);
    }

    @Test
    @DisplayName("I6: Transfer with Idempotency-Key returns cached response on retry")
    void transfer_withIdempotencyKey_shouldSucceed() throws Exception {
        RegisteredUser sender = registerEndpointUser("idemsend");
        RegisteredUser receiver = registerEndpointUser("idemrecv");
        Wallet senderWallet = createWalletFor(sender);
        Wallet receiverWallet = createWalletFor(receiver);

        // Set PIN for sender
        setPin(sender.token());

        // Fund sender
        mockMvc.perform(post("/v1/wallets/{walletId}/deposit", senderWallet.getId())
                        .header("Authorization", "Bearer " + sender.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100.00, \"description\": \"Fund sender\"}"))
                .andExpect(status().isCreated());

        String key = "idemp-transfer-1";
        String transferJson = """
                {"receiverWalletId": "%s", "amount": 30.00, "pin": "2468", "description": "Idempotent transfer"}
                """.formatted(receiverWallet.getId());

        String firstResponse = mockMvc.perform(post("/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(post("/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assertions.assertEquals(firstResponse, secondResponse);
    }
}
