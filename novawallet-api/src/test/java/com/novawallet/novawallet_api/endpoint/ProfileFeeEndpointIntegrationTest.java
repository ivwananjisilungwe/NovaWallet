package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.fee.entity.FeeConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileFeeEndpointIntegrationTest extends EndpointTestSupport {

    @Test
    @DisplayName("Admin can update fee configuration")
    void adminFeeUpdate_shouldPersistChanges() throws Exception {
        RegisteredUser user = registerEndpointUser("feeupdate");
        String adminToken = adminToken();

        FeeConfiguration transfer = feeConfigurationRepository.findAll().stream()
                .filter(config -> "TRANSFER".equals(config.getTransactionType().name()))
                .findFirst()
                .orElseThrow();

        String updateJson = """
                {
                    "transactionType": "TRANSFER",
                    "percentageFee": 0.0200,
                    "flatFee": 0.50,
                    "minFee": 1.00,
                    "maxFee": 100.00,
                    "active": true
                }
                """;

        mockMvc.perform(put("/v1/admin/fees/" + transfer.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.percentageFee").value(0.0200))
                .andExpect(jsonPath("$.data.flatFee").value(0.50))
                .andExpect(jsonPath("$.data.active").value(true));

        mockMvc.perform(put("/v1/admin/fees/" + transfer.getId())
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/admin/fees/" + transfer.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.percentageFee").value(0.0200));
    }

    @Test
    @DisplayName("GET/PUT profile and GET wallet/me enforce authenticated user contracts")
    void profileAndWalletEndpoints_shouldReturnCurrentUserData() throws Exception {
        RegisteredUser user = registerEndpointUser("profile");
        createWalletFor(user);

        mockMvc.perform(get("/v1/users/me")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(user.email()))
                .andExpect(jsonPath("$.data.role").value("USER"));

        String updateJson = """
                {"firstName":"Updated","lastName":"Tester","phone":"+260955000001"}
                """;
        mockMvc.perform(put("/v1/users/me")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Updated"))
                .andExpect(jsonPath("$.data.lastName").value("Tester"))
                .andExpect(jsonPath("$.data.phone").value("+260955000001"));

        mockMvc.perform(get("/v1/wallets/me")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.currency").value("ZMW"));

        mockMvc.perform(put("/v1/users/me")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Fee endpoints estimate public fees and protect admin configuration routes")
    void feeEndpoints_shouldEstimateAndProtectAdminConfiguration() throws Exception {
        RegisteredUser user = registerEndpointUser("fee");
        String adminToken = adminToken();
        FeeConfiguration transfer = feeConfigurationRepository.findAll().stream()
                .filter(config -> "TRANSFER".equals(config.getTransactionType().name()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/v1/fees/estimate")
                        .header("Authorization", "Bearer " + user.token())
                        .param("type", "TRANSFER")
                        .param("amount", "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.data.totalFee").value(1.00));

        mockMvc.perform(get("/v1/fees/estimate")
                        .header("Authorization", "Bearer " + user.token())
                        .param("type", "unknown")
                        .param("amount", "100.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/v1/admin/fees")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/admin/fees")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        mockMvc.perform(get("/v1/admin/fees/" + transfer.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("TRANSFER"));
    }
}
