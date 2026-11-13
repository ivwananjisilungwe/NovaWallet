package com.novawallet.novawallet_api.endpoint;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import com.novawallet.novawallet_api.notification.entity.Notification;
import com.novawallet.novawallet_api.notification.entity.NotificationChannel;
import com.novawallet.novawallet_api.notification.entity.NotificationType;
import com.novawallet.novawallet_api.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization gap coverage: notification scoping, PIN change, KYC upload
 * anonymous access, and admin document download with a cross-user document id.
 */
class AuthorizationGapIntegrationTest extends EndpointTestSupport {

    @Autowired
    private NotificationRepository notificationRepository;

    private RegisteredUser userA;
    private RegisteredUser userB;

    @BeforeEach
    void setUpUsers() throws Exception {
        userA = registerEndpointUser("authzgapa");
        userB = registerEndpointUser("authzgapb");
        verifyEmailFor(userA);
        verifyEmailFor(userB);
    }

    private String bearer(RegisteredUser user) {
        return "Bearer " + user.token();
    }

    private void saveNotificationsFor(RegisteredUser user, int count) {
        for (int i = 0; i < count; i++) {
            notificationRepository.save(Notification.builder()
                    .userId(user.userId())
                    .type(NotificationType.TRANSACTION_DEPOSIT)
                    .channel(NotificationChannel.EMAIL)
                    .recipient(user.email())
                    .subject("authz gap " + i)
                    .message("authz gap message " + i)
                    .createdAt(LocalDateTime.now().minusSeconds(i))
                    .build());
        }
    }

    @Test
    @DisplayName("notifications are scoped per user: B cannot list, count or mark-all-read A's notifications")
    void notifications_areScopedPerUser() throws Exception {
        // Registration creates each user's own notifications (e.g. email verification),
        // so assert against per-user baselines and A's marker subjects, not absolute zero.
        long baselineAll = notificationRepository.findByUserIdOrderByCreatedAtDesc(userA.userId()).size();
        long baselineUnreadA = notificationRepository.countByUserIdAndReadAtIsNull(userA.userId());
        long baselineUnreadB = notificationRepository.countByUserIdAndReadAtIsNull(userB.userId());
        saveNotificationsFor(userA, 2);

        mockMvc.perform(get("/api/v1/notifications").param("size", "100")
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("authz gap"))));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(baselineUnreadB));

        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isOk());

        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(userA.userId()))
                .filteredOn(n -> n.getSubject() != null && n.getSubject().startsWith("authz gap"))
                .hasSize(2)
                .allSatisfy(n -> assertThat(n.getReadAt()).isNull());

        mockMvc.perform(get("/api/v1/notifications").param("size", "100")
                        .header("Authorization", bearer(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value((int) baselineAll + 2))
                .andExpect(content().string(containsString("authz gap 0")))
                .andExpect(content().string(containsString("authz gap 1")));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", bearer(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(baselineUnreadA + 2));
    }

    @Test
    @DisplayName("PIN change: anonymous is 401")
    void pinChange_anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/pin/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPin\":\"1234\",\"newPin\":\"5678\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PIN change: authenticated invalid body is 400")
    void pinChange_invalidBody_isBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/pin/change")
                        .header("Authorization", bearer(userA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPin\":\"12\",\"newPin\":\"abc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("KYC document upload: anonymous multipart is 401")
    void kycUpload_anonymous_isUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "national-id.pdf", "application/pdf",
                "fake-pdf-content".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/kyc/documents/upload")
                        .file(file)
                        .param("documentType", "NATIONAL_ID"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("admin document download rejects a document that belongs to a different user")
    void adminDocumentDownload_crossUserDocument_isForbidden() throws Exception {
        UUID documentOwnedByA = uploadKycDocument(userA);

        mockMvc.perform(get("/api/v1/admin/kyc/" + userB.userId() + "/documents/" + documentOwnedByA)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("fake-pdf-content"))));
    }
}
