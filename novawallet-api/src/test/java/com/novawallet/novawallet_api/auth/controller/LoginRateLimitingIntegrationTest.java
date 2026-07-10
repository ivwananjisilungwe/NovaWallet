package com.novawallet.novawallet_api.auth.controller;

import com.novawallet.novawallet_api.auth.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginRateLimitingIntegrationTest extends BaseAuthIntegrationTest {

    @Test
    @DisplayName("Lock after 5 failed login attempts")
    void shouldLockAfterMultipleFailedAttempts() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Rate", "Limit", testEmail, testPhone, "SecurePass@123"))))
                .andExpect(status().isCreated());

        for (int i = 0; i < 6; i++) {
            var result = mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + testEmail + "\",\"password\":\"WrongPass@" + i + "\"}"))
                    .andReturn();

            int status = result.getResponse().getStatus();
            String code = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("code").asText();

            if (i < 5) {
                assertThat(status).isEqualTo(401);
                assertThat(code).isEqualTo("UNAUTHORIZED");
            } else {
                assertThat(status).isEqualTo(429);
                assertThat(code).isEqualTo("RATE_LIMITED");
            }
        }
    }

    @Test
    @DisplayName("Successful login resets failed attempt counter")
    void shouldAllowLoginAfterSuccessfulAttemptResetsCounter() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Rate", "Reset", testEmail, testPhone, "SecurePass@123"))))
                .andExpect(status().isCreated());

        // Fail 3 times
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + testEmail + "\",\"password\":\"WrongPass@" + i + "\"}"))
                    .andExpect(status().isUnauthorized());
        }

        // Success resets counter
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + testEmail + "\",\"password\":\"SecurePass@123\"}"))
                .andExpect(status().isOk());

        // More failures should work (counter was reset)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + testEmail + "\",\"password\":\"WrongPass@" + i + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
