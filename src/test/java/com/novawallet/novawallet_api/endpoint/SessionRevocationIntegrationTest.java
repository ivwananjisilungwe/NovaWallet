package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.user.dto.request.ChangePasswordRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-02: password change and admin deactivation revoke active refresh tokens,
 * while reactivation deliberately does not.
 */
class SessionRevocationIntegrationTest extends EndpointTestSupport {

    @Test
    @DisplayName("Changing password revokes the old refresh token")
    void passwordChangeRevokesRefreshToken() throws Exception {
        RegisteredUser user = registerEndpointUser("sec02pwd");
        verifyEmailFor(user);

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.email() + "\",\"password\":\"SecurePass@123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.data.refreshToken");

        mockMvc.perform(post("/api/v1/users/me/change-password")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"SecurePass@123\",\"newPassword\":\"NewPass@789\"}"))
                .andExpect(status().isOk());

        // The old refresh token no longer works.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // The new password works.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.email() + "\",\"password\":\"NewPass@789\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin deactivation revokes the user's refresh tokens")
    void adminDeactivationRevokesRefreshTokens() throws Exception {
        RegisteredUser user = registerEndpointUser("sec02deact");
        verifyEmailFor(user);

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.email() + "\",\"password\":\"SecurePass@123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.data.refreshToken");

        mockMvc.perform(patch("/api/v1/admin/users/" + user.userId() + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
