package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.wallet.entity.Wallet;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionEdgeCaseEndpointIntegrationTest extends EndpointTestSupport {

    @Autowired
    private com.novawallet.novawallet_api.transaction.repository.TransactionRepository transactionRepository;

    @Test
    @DisplayName("Deposit validation rejects missing amount")
    void deposit_missingAmount_shouldReturnValidationError() throws Exception {
        RegisteredUser user = registerEndpointUser("missingamount");
        Wallet wallet = createWalletFor(user);

        mockMvc.perform(post("/api/v1/wallets/" + wallet.getId() + "/deposit")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Missing amount\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Direct deposit is unavailable and does not credit the wallet")
    void directDeposit_isUnavailable_doesNotCredit() throws Exception {
        RegisteredUser user = registerEndpointUser("directdeposit");
        Wallet wallet = createWalletFor(user);

        mockMvc.perform(post("/api/v1/wallets/" + wallet.getId() + "/deposit")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00,\"description\":\"Unsafe deposit\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));

        org.assertj.core.api.Assertions.assertThat(walletRepository.findById(wallet.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(java.math.BigDecimal.ZERO);
        org.assertj.core.api.Assertions.assertThat(transactionRepository.findAll().stream()
                .filter(tx -> wallet.getId().equals(tx.getReceiverWalletId()))
                .count()).isZero();
    }

    @Test
    @DisplayName("Direct deposit with Idempotency-Key is unavailable and creates no rows")
    void directDeposit_withIdempotencyKey_isUnavailable() throws Exception {
        RegisteredUser user = registerEndpointUser("directdepositidem");
        Wallet wallet = createWalletFor(user);

        mockMvc.perform(post("/api/v1/wallets/" + wallet.getId() + "/deposit")
                        .header("Authorization", "Bearer " + user.token())
                        .header("Idempotency-Key", "idem-deposit-contained")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00,\"description\":\"Unsafe deposit\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));

        org.assertj.core.api.Assertions.assertThat(walletRepository.findById(wallet.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(java.math.BigDecimal.ZERO);
        org.assertj.core.api.Assertions.assertThat(transactionRepository.findAll().stream()
                .filter(tx -> wallet.getId().equals(tx.getReceiverWalletId())
                        || wallet.getId().equals(tx.getSenderWalletId()))
                .count()).isZero();
    }

    @Test
    @DisplayName("Non-owner deposit is forbidden")
    void directDeposit_nonOwner_isForbidden() throws Exception {
        RegisteredUser owner = registerEndpointUser("depowner");
        RegisteredUser other = registerEndpointUser("depother");
        Wallet wallet = createWalletFor(owner);

        mockMvc.perform(post("/api/v1/wallets/" + wallet.getId() + "/deposit")
                        .header("Authorization", "Bearer " + other.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00,\"description\":\"Unsafe deposit\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        org.assertj.core.api.Assertions.assertThat(walletRepository.findById(wallet.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(java.math.BigDecimal.ZERO);
        org.assertj.core.api.Assertions.assertThat(transactionRepository.findAll().stream()
                .filter(tx -> wallet.getId().equals(tx.getReceiverWalletId())
                        || wallet.getId().equals(tx.getSenderWalletId()))
                .count()).isZero();
    }

    @Test
    @DisplayName("Deposit on unknown wallet returns 404")
    void directDeposit_unknownWallet_returnsNotFound() throws Exception {
        RegisteredUser user = registerEndpointUser("depunknown");
        UUID unknownWalletId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/wallets/" + unknownWalletId + "/deposit")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00,\"description\":\"Unsafe deposit\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Deposit on frozen wallet returns 400 Wallet is not active")
    void directDeposit_frozenWallet_returnsBadRequest() throws Exception {
        RegisteredUser user = registerEndpointUser("depfrozen");
        Wallet wallet = createWalletFor(user);
        String adminToken = adminToken();

        mockMvc.perform(post("/api/v1/admin/wallets/" + wallet.getId() + "/freeze")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"ADMIN_ACTION\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/wallets/" + wallet.getId() + "/deposit")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00,\"description\":\"Unsafe deposit\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Wallet is not active"));
    }

    @Test
    @DisplayName("Deposit rejects invalid wallet UUID path parameters")
    void deposit_invalidWalletId_shouldReturnBadRequest() throws Exception {
        RegisteredUser user = registerEndpointUser("baduuid");

        mockMvc.perform(post("/api/v1/wallets/not-a-uuid/deposit")
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

        mockMvc.perform(post("/api/v1/admin/wallets/" + wallet.getId() + "/freeze")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"ADMIN_ACTION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FROZEN"));

        mockMvc.perform(post("/api/v1/wallets/" + wallet.getId() + "/withdraw")
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

        mockMvc.perform(post("/api/v1/admin/wallets/" + senderWallet.getId() + "/freeze")
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

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Sender wallet is not active"));
    }
}
