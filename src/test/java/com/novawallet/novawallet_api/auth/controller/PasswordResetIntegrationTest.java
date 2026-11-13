package com.novawallet.novawallet_api.auth.controller;

import com.novawallet.novawallet_api.auth.dto.request.RegisterRequest;
import com.novawallet.novawallet_api.notification.MailService;
import com.novawallet.novawallet_api.notification.entity.Notification;
import com.novawallet.novawallet_api.notification.repository.NotificationRepository;
import com.novawallet.novawallet_api.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for the password reset flow after SEC-03.
 *
 * <p>The raw reset token is delivered by email only. The database stores a
 * 44-character SHA-256 Base64 hash, and no notification row may contain the
 * raw value.</p>
 */
class PasswordResetIntegrationTest extends BaseAuthIntegrationTest {

    private static final int RAW_TOKEN_LENGTH = 43;
    private static final int HASHED_TOKEN_LENGTH = 44;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private MailService mailService;

    private String rawResetToken;
    private UUID userId;

    @BeforeEach
    void setupUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Reset", "Edge", testEmail, testPhone, "SecurePass@123"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + testEmail + "\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService, timeout(2000))
                .sendPasswordResetEmail(eq(testEmail), any(), tokenCaptor.capture());
        rawResetToken = tokenCaptor.getValue();

        User user = userRepository.findByEmail(testEmail).orElseThrow();
        userId = user.getId();
    }

    @Test
    @DisplayName("Reset password with the raw token from the delivered email")
    void shouldResetPassword() throws Exception {
        mockMvc.perform(post("/api/v1/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawResetToken + "\",\"newPassword\":\"NewReset@123\"}"))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(user.getPasswordResetToken()).isNull();
        assertThat(user.getPasswordResetExpiresAt()).isNull();

        // The new credential works and the old one does not.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + testEmail + "\",\"password\":\"NewReset@123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Reject invalid reset token")
    void shouldRejectInvalidResetToken() throws Exception {
        mockMvc.perform(post("/api/v1/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"invalid-token\",\"newPassword\":\"NewReset@123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Reject weak reset password")
    void shouldRejectWeakResetPassword() throws Exception {
        mockMvc.perform(post("/api/v1/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawResetToken + "\",\"newPassword\":\"weak\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("SEC-03: database stores a SHA-256 hash, never the raw reset token")
    void databaseStoresHashNotRawToken() {
        User user = userRepository.findByEmail(testEmail).orElseThrow();
        String stored = user.getPasswordResetToken();

        assertThat(stored).isNotNull();
        assertThat(stored).hasSize(HASHED_TOKEN_LENGTH);
        assertThat(rawResetToken).hasSize(RAW_TOKEN_LENGTH);
        assertThat(stored).isNotEqualTo(rawResetToken);
    }

    @Test
    @DisplayName("SEC-03: notification inbox body never contains the raw reset token")
    void notificationBodyNeverContainsRawToken() {
        List<Notification> notifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        assertThat(notifications).isNotEmpty();
        assertThat(notifications)
                .allSatisfy(notification -> {
                    assertThat(notification.getSubject()).doesNotContain(rawResetToken);
                    assertThat(notification.getMessage()).doesNotContain(rawResetToken);
                });
    }
}
