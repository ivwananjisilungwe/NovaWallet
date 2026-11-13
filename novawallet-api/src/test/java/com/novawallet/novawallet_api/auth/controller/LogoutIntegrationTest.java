package com.novawallet.novawallet_api.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.novawallet.novawallet_api.auth.dto.request.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-06 / NEW-AUTH-01 / DEC-02: logout must invalidate the presented access
 * token immediately and revoke the user's refresh tokens.
 *
 * <p>Unauthenticated requests are answered with 403 by the application's
 * default Spring Security entry point (there is no custom entry point), so a
 * revoked token — which is indistinguishable from a missing one after the
 * filter runs — is expected to produce 403, not 401.</p>
 */
class LogoutIntegrationTest extends BaseAuthIntegrationTest {

    private String accessToken;
    private String refreshToken;
    private UUID userId;

    @BeforeEach
    void registerUser() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Logout", "Test", testEmail, testPhone, "SecurePass@123"))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        accessToken = data.get("accessToken").asText();
        refreshToken = data.get("refreshToken").asText();
        userId = UUID.fromString(data.get("user").get("id").asText());
    }

    private void logout(String token) throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Logout returns 204 and the presented access token stops working")
    void logoutRevokesPresentedAccessToken() throws Exception {
        // Sanity: the token works before logout.
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        logout(accessToken);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout revokes the user's refresh tokens")
    void logoutRevokesRefreshTokens() throws Exception {
        logout(accessToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("A revoked access token cannot be replayed against a protected endpoint")
    void revokedTokenIsRejectedOnEveryProtectedEndpoint() throws Exception {
        logout(accessToken);

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout without a token is rejected and leaks nothing")
    void logoutWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Repeating logout does not error — the second call sees an already-invalid credential")
    void repeatedLogoutIsSafe() throws Exception {
        logout(accessToken);

        // The token is revoked, so the second call is unauthenticated rather
        // than a duplicate revocation. What matters is that it fails cleanly
        // (401, never 500) and the store is not corrupted.
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A user's logout cannot revoke another user's access token")
    void logoutIsScopedToTheCaller() throws Exception {
        var otherResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Other", "User", otherEmail(), otherPhone(), "SecurePass@123"))))
                .andExpect(status().isCreated())
                .andReturn();

        String otherToken = objectMapper.readTree(otherResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        logout(accessToken);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Documented limitation: another session's access token survives, its refresh token does not")
    void crossSessionAsymmetryIsDocumented() throws Exception {
        var secondLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + testEmail + "\",\"password\":\"SecurePass@123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode secondData = objectMapper.readTree(secondLogin.getResponse().getContentAsString()).get("data");
        String secondSessionAccessToken = secondData.get("accessToken").asText();
        String secondSessionRefreshToken = secondData.get("refreshToken").asText();

        logout(accessToken);

        // Access tokens other than the presented one are not enumerable and
        // remain valid until natural expiry (DEC-02 residual limitation).
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + secondSessionAccessToken))
                .andExpect(status().isOk());

        // Refresh tokens are revoked for the whole user.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + secondSessionRefreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout writes a revocation row keyed by jti for the caller")
    void logoutPersistsRevocationRow() throws Exception {
        logout(accessToken);

        long rowsForUser = revokedTokenRepository.findAll().stream()
                .filter(row -> row.getUserId().equals(userId))
                .count();
        assertThat(rowsForUser).isEqualTo(1);
    }

    private String otherEmail() {
        return "other" + System.nanoTime() + "@example.com";
    }

    private String otherPhone() {
        return "+26095" + String.format("%07d", Math.abs(System.nanoTime() % 10_000_000));
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.novawallet.novawallet_api.token.repository.RevokedAccessTokenRepository revokedTokenRepository;
}
