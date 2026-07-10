package com.novawallet.novawallet_api.auth.controller;

import com.novawallet.novawallet_api.auth.dto.request.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminSecurityIntegrationTest extends BaseAuthIntegrationTest {

    private String userToken;

    @BeforeEach
    void setupUser() throws Exception {
        var result = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Regular", "User", testEmail, testPhone, "SecurePass@123"))))
                .andExpect(status().isCreated())
                .andReturn();

        userToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("Reject non-admin user from admin endpoints")
    void shouldRejectNonAdminFromAdminEndpoints() throws Exception {
        mockMvc.perform(get("/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Reject unauthenticated request from admin endpoints")
    void shouldRejectUnauthenticatedFromAdminEndpoints() throws Exception {
        mockMvc.perform(get("/v1/admin/users"))
                .andExpect(status().isForbidden());
    }
}
