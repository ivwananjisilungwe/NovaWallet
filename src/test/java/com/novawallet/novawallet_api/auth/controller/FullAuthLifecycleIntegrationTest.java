package com.novawallet.novawallet_api.auth.controller;

import com.novawallet.novawallet_api.auth.dto.request.*;
import com.novawallet.novawallet_api.notification.MailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FullAuthLifecycleIntegrationTest extends BaseAuthIntegrationTest {

    /**
     * SEC-03 stores only a hash of the verification/reset token, so the raw
     * value must be captured from the delivered email — it cannot be read
     * back from the database.
     */
    @MockBean
    private MailService mailService;

    @Test
    @DisplayName("Register → verify email → login → refresh → profile → pin → forgot/reset password")
    void shouldCompleteFullAuthLifecycle() throws Exception {
        // ========== 1. REGISTER ==========
        RegisterRequest registerRequest = new RegisterRequest(
                "Flow", "Test", testEmail, testPhone, "SecurePass@123"
        );

        var registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.user.email").value(testEmail))
                .andExpect(jsonPath("$.data.user.emailVerified").value(false))
                .andExpect(jsonPath("$.data.user.pinSet").value(false))
                .andReturn();

        String accessToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
        String refreshToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("data").get("refreshToken").asText();

        // ========== 2. VERIFY EMAIL ==========
        ArgumentCaptor<String> verificationCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService, timeout(2000))
                .sendVerificationEmail(eq(testEmail), any(), verificationCaptor.capture());
        String verificationToken = verificationCaptor.getValue();
        assertThat(verificationToken).hasSize(43);

        mockMvc.perform(post("/api/v1/email/verify").param("token", verificationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        // Already verified fails
        mockMvc.perform(post("/api/v1/email/verify").param("token", verificationToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // ========== 3. LOGIN ==========
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(testEmail, "SecurePass@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Wrong password
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(testEmail, "WrongPass@456"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // Non-existent email
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("nonexistent@example.com", "SomePass@123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // ========== 4. REFRESH TOKEN ==========
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // ========== 5. GET PROFILE ==========
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(testEmail));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        // ========== 6. UPDATE PROFILE ==========
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content("{\"firstName\": \"UpdatedFlow\", \"lastName\": \"UpdatedTest\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("UpdatedFlow"));

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content("{\"firstName\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // ========== 7. SET PIN ==========
        mockMvc.perform(post("/api/v1/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(new SetPinRequest("2468"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("PIN set successfully"));

        mockMvc.perform(post("/api/v1/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SetPinRequest("2468"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .content(objectMapper.writeValueAsString(new SetPinRequest("2468"))))
                .andExpect(status().isUnauthorized());

        // ========== 8. FORGOT & RESET PASSWORD ==========
        mockMvc.perform(post("/api/v1/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(testEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the email exists, a reset link has been sent"));

        // Non-existent email also 200 (anti-enumeration)
        mockMvc.perform(post("/api/v1/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("nonexistent@example.com"))))
                .andExpect(status().isOk());

        // Extract the raw reset token from the delivered email (the DB holds a hash)
        ArgumentCaptor<String> resetCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService, timeout(2000))
                .sendPasswordResetEmail(eq(testEmail), any(), resetCaptor.capture());
        String resetToken = resetCaptor.getValue();
        assertThat(resetToken).hasSize(43);

        // The raw reset token is never stored in the database
        String storedResetToken = userRepository.findByEmail(testEmail).orElseThrow()
                .getPasswordResetToken();
        assertThat(storedResetToken).hasSize(44).isNotEqualTo(resetToken);

        // Reset password
        mockMvc.perform(post("/api/v1/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(resetToken, "NewPass@789"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));

        // Login with new password
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(testEmail, "NewPass@789"))))
                .andExpect(status().isOk());

        // Token already consumed
        mockMvc.perform(post("/api/v1/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(resetToken, "Another@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
