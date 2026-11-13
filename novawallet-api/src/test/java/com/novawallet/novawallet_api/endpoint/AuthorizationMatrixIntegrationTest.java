package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1C Batch 2 — IDOR / ownership matrix.
 *
 * <p>For every customer route that takes a resource identifier, user B must
 * never read or act on user A's resource; unauthenticated callers get 401;
 * customers can never reach {@code /api/v1/admin/**}.</p>
 */
class AuthorizationMatrixIntegrationTest extends EndpointTestSupport {

    @Autowired
    private TransactionRepository transactionRepository;

    private RegisteredUser owner;
    private RegisteredUser intruder;
    private Wallet ownerWallet;

    @BeforeEach
    void setUpUsers() throws Exception {
        owner = registerEndpointUser("idorowner");
        intruder = registerEndpointUser("idorintruder");
        verifyEmailFor(owner);
        verifyEmailFor(intruder);
        ownerWallet = createWalletFor(owner);
        ownerWallet.setBalance(new BigDecimal("500.00"));
        ownerWallet = walletRepository.save(ownerWallet);
        createWalletFor(intruder);
    }

    private String bearer(RegisteredUser user) {
        return "Bearer " + user.token();
    }

    @Nested
    @DisplayName("walletId routes")
    class WalletIdRoutes {

        @Test
        @DisplayName("owner can read own balance, history and statement")
        void owner_canRead() throws Exception {
            mockMvc.perform(get("/api/v1/wallets/" + ownerWallet.getId() + "/balance")
                            .header("Authorization", bearer(owner)))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/wallets/" + ownerWallet.getId() + "/transactions")
                            .header("Authorization", bearer(owner)))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/wallets/" + ownerWallet.getId() + "/statements")
                            .param("month", "2026-11")
                            .header("Authorization", bearer(owner)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("another customer is forbidden on balance, history and statement")
        void intruder_cannotRead() throws Exception {
            mockMvc.perform(get("/api/v1/wallets/" + ownerWallet.getId() + "/balance")
                            .header("Authorization", bearer(intruder)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/wallets/" + ownerWallet.getId() + "/transactions")
                            .header("Authorization", bearer(intruder)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/wallets/" + ownerWallet.getId() + "/statements")
                            .param("month", "2026-11")
                            .header("Authorization", bearer(intruder)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("another customer cannot withdraw from or deposit into someone else's wallet")
        void intruder_cannotMoveMoney() throws Exception {
            mockMvc.perform(post("/api/v1/wallets/" + ownerWallet.getId() + "/withdraw")
                            .header("Authorization", bearer(intruder))
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":10.00,\"pin\":\"1234\",\"description\":\"x\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/wallets/" + ownerWallet.getId() + "/deposit")
                            .header("Authorization", bearer(intruder))
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":10.00,\"description\":\"x\"}"))
                    .andExpect(status().isForbidden());

            Wallet after = walletRepository.findById(ownerWallet.getId()).orElseThrow();
            org.assertj.core.api.Assertions.assertThat(after.getBalance()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("unknown walletId is 404, malformed walletId is 400, never 200")
        void unknownAndMalformedIds() throws Exception {
            mockMvc.perform(get("/api/v1/wallets/" + UUID.randomUUID() + "/balance")
                            .header("Authorization", bearer(intruder)))
                    .andExpect(status().isNotFound());
            mockMvc.perform(get("/api/v1/wallets/not-a-uuid/balance")
                            .header("Authorization", bearer(intruder)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("unauthenticated calls are 401")
        void unauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/wallets/" + ownerWallet.getId() + "/balance"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/v1/wallets/" + ownerWallet.getId() + "/transactions"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(post("/api/v1/wallets/" + ownerWallet.getId() + "/payouts")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("transaction reference route")
    class ReferenceRoute {

        @Test
        @DisplayName("owner reads own transaction; another customer is forbidden; tampered reference is 404")
        void referenceOwnership() throws Exception {
            String reference = "NWTXIDOR" + (System.nanoTime() % 1_000_000_000L);
            transactionRepository.save(Transaction.builder()
                    .reference(reference)
                    .type(TransactionType.TRANSFER_DEBIT)
                    .amount(new BigDecimal("10.00"))
                    .status(TransactionStatus.SUCCESSFUL)
                    .description("idor test")
                    .senderWalletId(ownerWallet.getId())
                    .build());

            mockMvc.perform(get("/api/v1/transactions/" + reference)
                            .header("Authorization", bearer(owner)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reference").value(reference));
            mockMvc.perform(get("/api/v1/transactions/" + reference)
                            .header("Authorization", bearer(intruder)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/transactions/" + reference + "X")
                            .header("Authorization", bearer(owner)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("admin routes reject customers (single ADMIN role, DEC-03)")
    class AdminRoutes {

        @ParameterizedTest
        @ValueSource(strings = {
                "GET /api/v1/admin/users",
                "GET /api/v1/admin/users/{id}",
                "PATCH /api/v1/admin/users/{id}/deactivate",
                "GET /api/v1/admin/wallets",
                "POST /api/v1/admin/wallets/{id}/freeze",
                "POST /api/v1/admin/wallets/{id}/unfreeze",
                "GET /api/v1/admin/transactions",
                "GET /api/v1/admin/audit-logs",
                "GET /api/v1/admin/kyc/pending",
                "GET /api/v1/admin/kyc/{id}",
                "GET /api/v1/admin/kyc/{id}/documents/{id}",
                "POST /api/v1/admin/kyc/{id}/approve",
                "POST /api/v1/admin/kyc/{id}/reject",
                "GET /api/v1/admin/fees",
                "GET /api/v1/admin/fees/{id}",
                "POST /api/v1/admin/fees",
                "PUT /api/v1/admin/fees/{id}",
                "DELETE /api/v1/admin/fees/{id}",
                "GET /api/v1/admin/route-added-later"
        })
        void customerGets403_anonymousGets401(String route) throws Exception {
            String[] parts = route.split(" ");
            String path = parts[1].replace("{id}", UUID.randomUUID().toString());

            mockMvc.perform(request(parts[0], path).header("Authorization", bearer(owner)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(request(parts[0], path))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("admin reaches the admin user list")
        void adminAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isOk());
        }

        private MockHttpServletRequestBuilder request(String method, String path) {
            MockHttpServletRequestBuilder builder = switch (method) {
                case "GET" -> get(path);
                case "POST" -> post(path);
                case "PUT" -> put(path);
                case "PATCH" -> patch(path);
                case "DELETE" -> delete(path);
                default -> throw new IllegalArgumentException(method);
            };
            return builder.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
    }
}
