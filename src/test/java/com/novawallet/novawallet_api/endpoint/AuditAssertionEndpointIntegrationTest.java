package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.audit.entity.AuditLog;
import com.novawallet.novawallet_api.audit.repository.AuditLogRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditAssertionEndpointIntegrationTest extends EndpointTestSupport {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("AuditLog entity mapping and repository are wired correctly")
    void auditLogRepository_shouldPersistEntity() {
        AuditLog log = AuditLog.builder()
                .entityType("TestEntity")
                .entityId(UUID.randomUUID())
                .action("TEST_ACTION")
                .oldValue("old")
                .newValue("new")
                .build();
        AuditLog saved = auditLogRepository.save(log);
        assertNotNull(saved.getId());
        AuditLog found = auditLogRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("TEST_ACTION", found.getAction());
        assertEquals("TestEntity", found.getEntityType());
    }

    @Test
    @DisplayName("Withdrawal triggers async audit log entry via AuditService")
    void withdrawal_shouldCreateAuditLogEntry() throws Exception {
        RegisteredUser user = registerEndpointUser("auditdep");
        Wallet wallet = createWalletFor(user);
        verifyEmailFor(user);

        transactionTemplate.executeWithoutResult(status -> walletRepository.updateBalance(wallet.getId(), new BigDecimal("100.00")));

        mockMvc.perform(post("/api/v1/pin")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"1234\"}"))
                .andExpect(status().isOk());

        long before = auditLogRepository.count();

        mockMvc.perform(post("/api/v1/wallets/" + wallet.getId() + "/withdraw")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":25.00,\"pin\":\"1234\",\"description\":\"Audit withdrawal test\"}"))
                .andExpect(status().isCreated());

        long deadline = System.currentTimeMillis() + 5000;
        long after;
        while (true) {
            after = auditLogRepository.count();
            if (after > before) break;
            if (System.currentTimeMillis() >= deadline) {
                fail("Timed out waiting for async audit log to appear. "
                        + "Before: " + before + ", After: " + after);
            }
            Thread.sleep(200);
        }

        assertTrue(after > before,
                "Expected audit log count " + before + " to increase after withdrawal. Was: " + after);
    }
}
