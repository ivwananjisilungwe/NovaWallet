package com.novawallet.novawallet_api.auth.controller;

import com.jayway.jsonpath.JsonPath;
import com.novawallet.novawallet_api.auth.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RefreshTokenRotationIntegrationTest extends BaseAuthIntegrationTest {

    @Test
    @DisplayName("Full rotation cycle + reuse detection: fresh token refreshes, old revoked, reuse triggers family invalidation")
    void refreshTokenRotation_shouldRotateAndDetectReuse() throws Exception {
        // ========== 1. REGISTER ==========
        RegisterRequest registerRequest = new RegisterRequest(
                "Rotation", "Test", testEmail, testPhone, "SecurePass@123"
        );

        String registerResponse = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn().getResponse().getContentAsString();

        String rt1 = JsonPath.read(registerResponse, "$.data.refreshToken");

        // ========== 2. FIRST ROTATION (rt1 → rt2) ==========
        String refresh1Response = mockMvc.perform(post("/v1/auth/refresh")
                        .header("Authorization", "Bearer " + rt1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String rt2 = JsonPath.read(refresh1Response, "$.data.refreshToken");
        assertThat(rt2).isNotEqualTo(rt1);

        // ========== 3. SECOND ROTATION (rt2 → rt3) ==========
        String refresh2Response = mockMvc.perform(post("/v1/auth/refresh")
                        .header("Authorization", "Bearer " + rt2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String rt3 = JsonPath.read(refresh2Response, "$.data.refreshToken");
        assertThat(rt3).isNotEqualTo(rt2);
        assertThat(rt3).isNotEqualTo(rt1);

        // ========== 4. REUSE DETECTION: old revoked token (rt1) → revokeAllByUserId ==========
        mockMvc.perform(post("/v1/auth/refresh")
                        .header("Authorization", "Bearer " + rt1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));

        // ========== 5. ALL TOKENS NOW REVOKED (step 4 triggered family invalidation) ==========
        mockMvc.perform(post("/v1/auth/refresh")
                        .header("Authorization", "Bearer " + rt2))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/v1/auth/refresh")
                        .header("Authorization", "Bearer " + rt3))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
