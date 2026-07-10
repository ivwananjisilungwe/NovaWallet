package com.novawallet.novawallet_api.transaction.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.common.dto.PagedResponse;
import com.novawallet.novawallet_api.transaction.dto.TransactionResponse;
import com.novawallet.novawallet_api.wallet.dto.WalletResponse;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    private static int counter = 0;

    private String user1Email;
    private String user1Token;
    private UUID user1WalletId;
    private String user1AccountNumber;

    private String user2Email;
    private String user2Token;
    private UUID user2WalletId;

    @BeforeEach
    void setUp() throws Exception {
        counter++;

        // ===== Register user1 =====
        user1Email = "tx.user1." + counter + "@example.com";
        String user1Phone = "+26097100" + String.format("%04d", counter);
        user1Token = registerAndGetToken(user1Email, user1Phone);

        // Create wallet for user1 directly (bypass KYC for testing)
        Wallet wallet1 = Wallet.builder()
                .userId(extractUserId(user1Token))
                .accountNumber("NW100" + String.format("%07d", counter))
                .balance(BigDecimal.ZERO.setScale(2))
                .currency("ZMW")
                .build();
        wallet1 = walletRepository.save(wallet1);
        user1WalletId = wallet1.getId();
        user1AccountNumber = wallet1.getAccountNumber();

        // ===== Register user2 =====
        user2Email = "tx.user2." + counter + "@example.com";
        String user2Phone = "+26097200" + String.format("%04d", counter);
        user2Token = registerAndGetToken(user2Email, user2Phone);

        // Create wallet for user2
        Wallet wallet2 = Wallet.builder()
                .userId(extractUserId(user2Token))
                .accountNumber("NW200" + String.format("%07d", counter))
                .balance(BigDecimal.ZERO.setScale(2))
                .currency("ZMW")
                .build();
        wallet2 = walletRepository.save(wallet2);
        user2WalletId = wallet2.getId();
    }

    // ==================== Full Transaction Lifecycle ====================

    @Nested
    @DisplayName("Deposit → Balance → History → Withdraw → Transfer")
    class TransactionLifecycle {

        @Test
        void shouldCompleteDepositWithdrawAndTransfer() throws Exception {
            // =========================================
            // 1. DEPOSIT K100
            // =========================================
            String depositJson = """
                    {"amount": 100.00, "description": "Cash deposit"}
                    """;

            MvcResult depositResult = mockMvc.perform(post("/api/v1/wallets/" + user1WalletId + "/deposit")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(depositJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.type").value("DEPOSIT"))
                    .andExpect(jsonPath("$.data.amount").value(100.00))
                    .andExpect(jsonPath("$.data.balanceAfter").value(100.00))
                    .andReturn();

            String depositRef = objectMapper.readTree(depositResult.getResponse().getContentAsString())
                    .get("data").get("reference").asText();

            // =========================================
            // 2. CHECK BALANCE
            // =========================================
            MvcResult balanceResult = mockMvc.perform(get("/api/v1/wallets/" + user1WalletId + "/balance")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.balance").value(100.00))
                    .andExpect(jsonPath("$.data.currency").value("ZMW"))
                    .andReturn();

            // =========================================
            // 3. TRANSACTION HISTORY (should have 1 deposit)
            // =========================================
            MvcResult historyResult = mockMvc.perform(get("/api/v1/wallets/" + user1WalletId + "/transactions")
                            .header("Authorization", "Bearer " + user1Token)
                            .param("type", "DEPOSIT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.content[0].reference").value(depositRef))
                    .andReturn();

            // =========================================
            // 4. SINGLE TRANSACTION LOOKUP
            // =========================================
            mockMvc.perform(get("/api/v1/transactions/" + depositRef)
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reference").value(depositRef))
                    .andExpect(jsonPath("$.data.type").value("DEPOSIT"));

            // =========================================
            // 5. SET PIN (needed for withdraw/transfer)
            // =========================================
            String setPinJson = """
                    {"pin": "1234"}
                    """;
            mockMvc.perform(post("/v1/pin")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(setPinJson))
                    .andExpect(status().isOk());

            // =========================================
            // 6. WITHDRAW K30
            // =========================================
            String withdrawJson = """
                    {"amount": 30.00, "pin": "1234", "description": "ATM withdrawal"}
                    """;

            mockMvc.perform(post("/api/v1/wallets/" + user1WalletId + "/withdraw")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(withdrawJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.type").value("WITHDRAWAL"))
                    .andExpect(jsonPath("$.data.amount").value(30.00))
                    .andExpect(jsonPath("$.data.balanceAfter").value(70.00));

            // =========================================
            // 7. TRANSFER K20 to user2
            // =========================================
            String transferJson = """
                    {"receiverWalletId": "%s", "amount": 20.00, "pin": "1234", "description": "Payment"}
                    """.formatted(user2WalletId.toString());

            MvcResult transferResult = mockMvc.perform(post("/api/v1/transfers")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(transferJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.type").value("TRANSFER_DEBIT"))
                    .andExpect(jsonPath("$.data.amount").value(20.00))
                    .andReturn();

            // =========================================
            // 8. VERIFY FINAL BALANCES
            // =========================================
            // user1: 100 - 30 - 20 = 50
            ApiResponse<WalletResponse> user1FinalBalance = objectMapper.readValue(
                    mockMvc.perform(get("/api/v1/wallets/" + user1WalletId + "/balance")
                                    .header("Authorization", "Bearer " + user1Token))
                            .andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );
            // 100 - 30 - 0.50(withdrawal fee) - 20 - 1.00(transfer fee) = 48.50
            assertThat(user1FinalBalance.data().balance()).isEqualByComparingTo("48.50");

            // user2: 0 + 20 = 20
            ApiResponse<WalletResponse> user2FinalBalance = objectMapper.readValue(
                    mockMvc.perform(get("/api/v1/wallets/" + user2WalletId + "/balance")
                                    .header("Authorization", "Bearer " + user2Token))
                            .andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );
            assertThat(user2FinalBalance.data().balance()).isEqualByComparingTo("20.00");

            // =========================================
            // 9. HISTORY FILTERING — user1 has 3 transactions
            // =========================================
            String allHistory = mockMvc.perform(get("/api/v1/wallets/" + user1WalletId + "/transactions")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            ApiResponse<PagedResponse<TransactionResponse>> historyResponse = objectMapper.readValue(
                    allHistory, new TypeReference<>() {});
            // deposit + withdraw + fee(1) + transfer_debit + transfer_credit + fee(2) = 6
            assertThat(historyResponse.data().totalElements()).isEqualTo(6);

            // =========================================
            // 10. OWNERSHIP ENFORCEMENT
            // =========================================
            // user2 cannot access user1's wallet
            mockMvc.perform(get("/api/v1/wallets/" + user1WalletId + "/balance")
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/wallets/" + user1WalletId + "/transactions")
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== Helpers ====================

    private String registerAndGetToken(String email, String phone) throws Exception {
        String registerJson = """
                {"firstName": "Tx", "lastName": "User", "email": "%s", "phone": "%s", "password": "SecurePass@123"}
                """.formatted(email, phone);

        MvcResult result = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("data").get("accessToken").asText();
    }

    private UUID extractUserId(String token) throws Exception {
        // Parse the JWT to get the username (which is the user ID)
        String payload = token.split("\\.")[1];
        byte[] decoded = java.util.Base64.getUrlDecoder().decode(payload);
        String json = new String(decoded);
        // {"sub":"<userId>", ...}
        String sub = objectMapper.readTree(json).get("sub").asText();
        return UUID.fromString(sub);
    }
}
