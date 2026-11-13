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
 * End-to-end coverage for the email verification flow after SEC-03.
 *
 * <p>The raw token exists only in the delivered email. The database stores a
 * 44-character SHA-256 Base64 hash, and no notification row may contain the
 * raw value.</p>
 */
class EmailVerificationIntegrationTest extends BaseAuthIntegrationTest {

    /** Output of {@code TokenService.generateSecureToken()} — 32 bytes Base64-URL, no padding. */
    private static final int RAW_TOKEN_LENGTH = 43;
    /** Output of {@code TokenService.hashToken(...)} — 32-byte SHA-256, Base64. */
    private static final int HASHED_TOKEN_LENGTH = 44;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private MailService mailService;

    private String rawVerificationToken;
    private UUID userId;

    @BeforeEach
    void setupUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Verify", "Edge", testEmail, testPhone, "SecurePass@123"))))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService, timeout(2000))
                .sendVerificationEmail(eq(testEmail), any(), tokenCaptor.capture());
        rawVerificationToken = tokenCaptor.getValue();

        User user = userRepository.findByEmail(testEmail).orElseThrow();
        userId = user.getId();
    }

    @Test
    @DisplayName("Verify email with the raw token from the delivered email")
    void shouldVerifyEmail() throws Exception {
        mockMvc.perform(post("/api/v1/email/verify").param("token", rawVerificationToken))
                .andExpect(status().isOk());

        User verified = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(verified.isEmailVerified()).isTrue();
        assertThat(verified.getVerificationToken()).isNull();
    }

    @Test
    @DisplayName("Reject already verified email")
    void shouldRejectAlreadyVerified() throws Exception {
        mockMvc.perform(post("/api/v1/email/verify").param("token", rawVerificationToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/email/verify").param("token", rawVerificationToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Reject invalid verification token")
    void shouldRejectInvalidToken() throws Exception {
        mockMvc.perform(post("/api/v1/email/verify").param("token", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Require token parameter")
    void shouldRequireTokenParam() throws Exception {
        mockMvc.perform(post("/api/v1/email/verify"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SEC-03: database stores a SHA-256 hash, never the raw token")
    void databaseStoresHashNotRawToken() {
        User user = userRepository.findByEmail(testEmail).orElseThrow();
        String stored = user.getVerificationToken();

        assertThat(stored).isNotNull();
        assertThat(stored).hasSize(HASHED_TOKEN_LENGTH);
        assertThat(rawVerificationToken).hasSize(RAW_TOKEN_LENGTH);
        assertThat(stored).isNotEqualTo(rawVerificationToken);
        // A hash is not a raw token merely re-encoded: prefix/suffix must differ.
        assertThat(rawVerificationToken).doesNotContain(stored);
    }

    @Test
    @DisplayName("SEC-03: notification inbox body never contains the raw token")
    void notificationBodyNeverContainsRawToken() {
        List<Notification> notifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        assertThat(notifications).isNotEmpty();
        assertThat(notifications)
                .allSatisfy(notification -> {
                    assertThat(notification.getSubject()).doesNotContain(rawVerificationToken);
                    assertThat(notification.getMessage()).doesNotContain(rawVerificationToken);
                });
    }
}
