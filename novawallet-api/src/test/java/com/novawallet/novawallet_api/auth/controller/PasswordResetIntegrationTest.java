package com.novawallet.novawallet_api.auth.controller;

import com.novawallet.novawallet_api.auth.dto.request.RegisterRequest;
import com.novawallet.novawallet_api.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetIntegrationTest extends BaseAuthIntegrationTest {

    private String resetToken;

    @BeforeEach
    void setupUser() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Reset", "Edge", testEmail, testPhone, "SecurePass@123"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + testEmail + "\"}"))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(testEmail).orElseThrow();
        resetToken = user.getPasswordResetToken();
    }

    @Test
    @DisplayName("Reset password with valid token")
    void shouldResetPassword() throws Exception {
        mockMvc.perform(post("/v1/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"NewReset@123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Reject invalid reset token")
    void shouldRejectInvalidResetToken() throws Exception {
        mockMvc.perform(post("/v1/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"invalid-token\",\"newPassword\":\"NewReset@123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Reject weak reset password")
    void shouldRejectWeakResetPassword() throws Exception {
        mockMvc.perform(post("/v1/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"weak\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
