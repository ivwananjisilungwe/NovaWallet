package com.novawallet.novawallet_api.endpoint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KycNegativeEndpointIntegrationTest extends EndpointTestSupport {

    @Test
    @DisplayName("KYC upload rejects empty files")
    void uploadDocument_emptyFile_shouldReturnBadRequest() throws Exception {
        RegisteredUser user = registerEndpointUser("emptykyc");

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        mockMvc.perform(multipart("/v1/kyc/documents/upload")
                        .file(emptyFile)
                        .param("documentType", "NATIONAL_ID")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("KYC upload rejects unsupported MIME types")
    void uploadDocument_invalidMimeType_shouldReturnBadRequest() throws Exception {
        RegisteredUser user = registerEndpointUser("badmime");

        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "not-valid-kyc".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/v1/kyc/documents/upload")
                        .file(invalidFile)
                        .param("documentType", "NATIONAL_ID")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("KYC upload is blocked while review is pending")
    void uploadDocument_pendingReview_shouldReturnBadRequest() throws Exception {
        RegisteredUser user = registerEndpointUser("pendingkyc");

        uploadKycDocument(user);

        mockMvc.perform(post("/v1/kyc/submit")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kycStatus").value("PENDING"));

        MockMultipartFile anotherFile = new MockMultipartFile(
                "file",
                "another.pdf",
                "application/pdf",
                "pending-review-upload".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/v1/kyc/documents/upload")
                        .file(anotherFile)
                        .param("documentType", "PASSPORT")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("KYC is pending review. Cannot upload more documents until reviewed."));
    }
}
