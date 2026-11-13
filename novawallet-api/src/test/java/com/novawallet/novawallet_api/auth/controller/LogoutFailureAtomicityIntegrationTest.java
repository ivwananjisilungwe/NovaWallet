package com.novawallet.novawallet_api.auth.controller;

import com.novawallet.novawallet_api.auth.dto.request.RegisterRequest;
import com.novawallet.novawallet_api.token.repository.RevokedAccessTokenRepository;
import com.novawallet.novawallet_api.token.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LogoutFailureAtomicityIntegrationTest extends BaseAuthIntegrationTest {

    @Autowired
    private RevokedAccessTokenRepository revokedAccessTokenRepository;

    @SpyBean
    private TokenService tokenService;

    @Test
    @DisplayName("If refresh-token revocation fails, access-token revocation rolls back and recovery remains possible")
    void failedRefreshRevocationRollsBackAccessRevocation() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Atomic", "Logout", testEmail, testPhone, "SecurePass@123"))))
                .andExpect(status().isCreated())
                .andReturn();

        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
        String refreshToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("refreshToken").asText();
        UUID userId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("user").get("id").asText());

        doThrow(new IllegalStateException("forced refresh-token revocation failure"))
                .when(tokenService).revokeAllUserTokensInCurrentTransaction(any());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isInternalServerError());

        assertThat(revokedAccessTokenRepository.findAll().stream()
                .filter(row -> row.getUserId().equals(userId))
                .count()).isZero();

        // The refresh token was not revoked, so the client can mint a new
        // access token and retry logout successfully.
        var refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andReturn();

        String replacementAccessToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        doNothing().when(tokenService).revokeAllUserTokensInCurrentTransaction(any());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + replacementAccessToken))
                .andExpect(status().isNoContent());

        assertThat(revokedAccessTokenRepository.findAll().stream()
                .filter(row -> row.getUserId().equals(userId))
                .count()).isEqualTo(1);
    }
}