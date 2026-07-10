package com.novawallet.novawallet_api.auth.controller;

import com.novawallet.novawallet_api.auth.dto.request.RegisterRequest;
import com.novawallet.novawallet_api.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmailVerificationIntegrationTest extends BaseAuthIntegrationTest {

    private String verificationToken;

    @BeforeEach
    void setupUser() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Verify", "Edge", testEmail, testPhone, "SecurePass@123"))))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(testEmail).orElseThrow();
        verificationToken = user.getVerificationToken();
    }

    @Test
    @DisplayName("Verify email with valid token")
    void shouldVerifyEmail() throws Exception {
        mockMvc.perform(post("/v1/email/verify").param("token", verificationToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Reject already verified email")
    void shouldRejectAlreadyVerified() throws Exception {
        mockMvc.perform(post("/v1/email/verify").param("token", verificationToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/email/verify").param("token", verificationToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Reject invalid verification token")
    void shouldRejectInvalidToken() throws Exception {
        mockMvc.perform(post("/v1/email/verify").param("token", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Require token parameter")
    void shouldRequireTokenParam() throws Exception {
        mockMvc.perform(post("/v1/email/verify"))
                .andExpect(status().isBadRequest());
    }
}
