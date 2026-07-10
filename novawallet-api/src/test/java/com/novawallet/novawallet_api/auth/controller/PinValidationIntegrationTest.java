package com.novawallet.novawallet_api.auth.controller;

import com.novawallet.novawallet_api.auth.dto.request.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PinValidationIntegrationTest extends BaseAuthIntegrationTest {

    private String accessToken;

    @BeforeEach
    void setupUser() throws Exception {
        var result = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Pin", "Test", testEmail, testPhone, "SecurePass@123"))))
                .andExpect(status().isCreated())
                .andReturn();

        accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("Reject PIN shorter than 4 digits")
    void shouldRejectShortPin() throws Exception {
        mockMvc.perform(post("/v1/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content("{\"pin\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Reject PIN longer than 6 digits")
    void shouldRejectLongPin() throws Exception {
        mockMvc.perform(post("/v1/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content("{\"pin\":\"1234567\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Reject non-numeric PIN")
    void shouldRejectNonNumericPin() throws Exception {
        mockMvc.perform(post("/v1/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content("{\"pin\":\"abcd\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Accept valid 4-digit PIN")
    void shouldSetPinSuccessfully() throws Exception {
        mockMvc.perform(post("/v1/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content("{\"pin\":\"2468\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("PIN set successfully"));
    }

    @Test
    @DisplayName("Reject PIN without authentication")
    void shouldRejectPinWithoutAuth() throws Exception {
        mockMvc.perform(post("/v1/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"2468\"}"))
                .andExpect(status().isForbidden());
    }
}
