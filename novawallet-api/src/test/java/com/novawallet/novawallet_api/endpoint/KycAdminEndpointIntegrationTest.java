package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.wallet.entity.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KycAdminEndpointIntegrationTest extends EndpointTestSupport {

    @Test
    @DisplayName("KYC endpoints upload, submit, expose admin review, and approve into wallet creation")
    void kycEndpoints_shouldSupportSubmitAndAdminApprovalFlow() throws Exception {
        RegisteredUser user = registerEndpointUser("kycapprove");
        String adminToken = adminToken();

        mockMvc.perform(get("/v1/kyc/status")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kycStatus").value("NOT_SUBMITTED"));

        var documentId = uploadKycDocument(user);

        mockMvc.perform(post("/v1/kyc/submit")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kycStatus").value("PENDING"));

        mockMvc.perform(get("/v1/admin/kyc/pending")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + user.userId() + "')]").exists());

        mockMvc.perform(get("/v1/admin/kyc/" + user.userId() + "/documents/" + documentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes("fake-pdf-content".getBytes()));

        mockMvc.perform(post("/v1/admin/kyc/" + user.userId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tier\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("KYC approved and wallet created"));

        mockMvc.perform(get("/v1/wallets/me")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Admin endpoints reject KYC, toggle users, and freeze/unfreeze wallets")
    void adminEndpoints_shouldCoverReviewToggleAndWalletFreezeFlow() throws Exception {
        RegisteredUser user = registerEndpointUser("adminflow");
        Wallet wallet = createWalletFor(user);
        String adminToken = adminToken();

        uploadKycDocument(user);
        mockMvc.perform(post("/v1/kyc/submit")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == '" + user.userId() + "')]").exists());

        mockMvc.perform(get("/v1/admin/users/" + user.userId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(user.email()));

        mockMvc.perform(get("/v1/admin/kyc/" + user.userId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kycStatus").value("PENDING"));

        mockMvc.perform(post("/v1/admin/kyc/" + user.userId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Document is blurry\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("KYC rejected"));

        mockMvc.perform(post("/v1/admin/wallets/" + wallet.getId() + "/freeze")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"ADMIN_ACTION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FROZEN"))
                .andExpect(jsonPath("$.data.freezeReason").value("ADMIN_ACTION"));

        mockMvc.perform(post("/v1/admin/wallets/" + wallet.getId() + "/unfreeze")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.freezeReason").doesNotExist());

        mockMvc.perform(patch("/v1/admin/users/" + user.userId() + "/toggle-delete")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User delete status toggled"));
    }
}
