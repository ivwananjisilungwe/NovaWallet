package com.novawallet.novawallet_api.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.novawallet.novawallet_api.auth.controller.BaseAuthIntegrationTest;
import com.novawallet.novawallet_api.auth.dto.request.RegisterRequest;
import com.novawallet.novawallet_api.fee.repository.FeeConfigurationRepository;
import com.novawallet.novawallet_api.kyc.repository.KycDocumentRepository;
import com.novawallet.novawallet_api.security.JwtUtil;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class EndpointTestSupport extends BaseAuthIntegrationTest {

    private static final AtomicInteger ENDPOINT_COUNTER = new AtomicInteger();

    @Autowired
    protected WalletRepository walletRepository;

    @Autowired
    protected UserRepository endpointUserRepository;

    @Autowired
    protected KycDocumentRepository kycDocumentRepository;

    @Autowired
    protected FeeConfigurationRepository feeConfigurationRepository;

    @Autowired
    protected JwtUtil jwtUtil;

    protected RegisteredUser registerEndpointUser(String label) throws Exception {
        int suffix = ENDPOINT_COUNTER.incrementAndGet();
        String safeLabel = label.replaceAll("[^a-z0-9]", "");
        String email = safeLabel + ".endpoint." + suffix + "@example.com";
        String phone = "+26096" + String.format("%07d", suffix);

        RegisterRequest request = new RegisterRequest(
                "Endpoint", label,
                email,
                phone,
                "SecurePass@123"
        );

        MvcResult result = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        String token = data.get("accessToken").asText();
        UUID userId = jwtUtil.extractUserId(token);
        return new RegisteredUser(userId, email, phone, token);
    }

    protected String adminToken() {
        User admin = endpointUserRepository.findByEmail("admin@novawallet.com")
                .orElseThrow(() -> new IllegalStateException("Default admin user was not initialized"));
        return jwtUtil.generateToken(admin.getId(), admin.getEmail(), admin.getRole().name());
    }

    protected Wallet createWalletFor(RegisteredUser user) {
        int suffix = ENDPOINT_COUNTER.incrementAndGet();
        Wallet wallet = Wallet.builder()
                .userId(user.userId())
                .accountNumber("EP" + String.format("%018d", suffix))
                .balance(BigDecimal.ZERO.setScale(2))
                .currency("ZMW")
                .build();
        return walletRepository.save(wallet);
    }

    protected UUID uploadKycDocument(RegisteredUser user) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "national-id.pdf",
                "application/pdf",
                "fake-pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/v1/kyc/documents/upload")
                        .file(file)
                        .param("documentType", "NATIONAL_ID")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk());

        return kycDocumentRepository.findByUserId(user.userId()).get(0).getId();
    }

    protected record RegisteredUser(UUID userId, String email, String phone, String token) {}
}
