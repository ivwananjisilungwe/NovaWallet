package com.novawallet.novawallet_api.audit.service;

import com.novawallet.novawallet_api.audit.entity.AuditLog;
import com.novawallet.novawallet_api.audit.repository.AuditLogRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditService auditService;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    private UUID userId;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository);
        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();
    }

    // ==================== recordAction (with IP) ====================

    @Nested
    class RecordActionWithIp {

        @Test
        void shouldSaveAuditLogWithAllFields() {
            CompletableFuture<Void> future = auditService.recordAction(
                    "Wallet",
                    walletId,
                    "TRANSFER_DEBIT",
                    "500.00",
                    "450.00",
                    userId,
                    "192.168.1.1"
            );

            // Verify the CompletableFuture completes
            assertThat(future).isCompleted();

            verify(auditLogRepository).save(auditLogCaptor.capture());
            AuditLog saved = auditLogCaptor.getValue();

            assertThat(saved.getEntityType()).isEqualTo("Wallet");
            assertThat(saved.getEntityId()).isEqualTo(walletId);
            assertThat(saved.getAction()).isEqualTo("TRANSFER_DEBIT");
            assertThat(saved.getOldValue()).isEqualTo("500.00");
            assertThat(saved.getNewValue()).isEqualTo("450.00");
            assertThat(saved.getPerformedBy()).isEqualTo(userId);
            assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
        }

        @Test
        void shouldHandleNullOldValue() {
            auditService.recordAction(
                    "Wallet",
                    walletId,
                    "CREATE",
                    null,
                    "1000.00",
                    userId,
                    "10.0.0.1"
            );

            verify(auditLogRepository).save(auditLogCaptor.capture());
            AuditLog saved = auditLogCaptor.getValue();

            assertThat(saved.getOldValue()).isNull();
            assertThat(saved.getNewValue()).isEqualTo("1000.00");
        }

        @Test
        void shouldHandleNullNewValue() {
            auditService.recordAction(
                    "User",
                    userId,
                    "DELETE",
                    "active",
                    null,
                    userId,
                    null
            );

            verify(auditLogRepository).save(auditLogCaptor.capture());
            AuditLog saved = auditLogCaptor.getValue();

            assertThat(saved.getOldValue()).isEqualTo("active");
            assertThat(saved.getNewValue()).isNull();
            assertThat(saved.getIpAddress()).isNull();
        }

        @Test
        void shouldHandleNullEntityId() {
            auditService.recordAction(
                    "System",
                    null,
                    "STARTUP",
                    null,
                    null,
                    null,
                    null
            );

            verify(auditLogRepository).save(auditLogCaptor.capture());
            AuditLog saved = auditLogCaptor.getValue();

            assertThat(saved.getEntityType()).isEqualTo("System");
            assertThat(saved.getEntityId()).isNull();
            assertThat(saved.getAction()).isEqualTo("STARTUP");
            assertThat(saved.getPerformedBy()).isNull();
        }
    }

    // ==================== recordAction (without IP) ====================

    @Nested
    class RecordActionWithoutIp {

        @Test
        void shouldSaveAuditLogWithoutIpAddress() {
            auditService.recordAction(
                    "Wallet",
                    walletId,
                    "DEPOSIT",
                    "1000.00",
                    "1500.00",
                    userId
            );

            verify(auditLogRepository).save(auditLogCaptor.capture());
            AuditLog saved = auditLogCaptor.getValue();

            assertThat(saved.getEntityType()).isEqualTo("Wallet");
            assertThat(saved.getEntityId()).isEqualTo(walletId);
            assertThat(saved.getAction()).isEqualTo("DEPOSIT");
            assertThat(saved.getOldValue()).isEqualTo("1000.00");
            assertThat(saved.getNewValue()).isEqualTo("1500.00");
            assertThat(saved.getPerformedBy()).isEqualTo(userId);
            assertThat(saved.getIpAddress()).isNull();
        }

        @Test
        void shouldUseOverloadForAllEntityTypes() {
            // Test with different entity type scenarios
            auditService.recordAction(
                    "User",
                    UUID.randomUUID(),
                    "ROLE_CHANGE",
                    "USER",
                    "ADMIN",
                    userId
            );

            verify(auditLogRepository).save(auditLogCaptor.capture());
            AuditLog saved = auditLogCaptor.getValue();

            assertThat(saved.getEntityType()).isEqualTo("User");
            assertThat(saved.getAction()).isEqualTo("ROLE_CHANGE");
            assertThat(saved.getOldValue()).isEqualTo("USER");
            assertThat(saved.getNewValue()).isEqualTo("ADMIN");
        }
    }

    // ==================== Async Behavior ====================

    @Nested
    class AsyncBehavior {

        @Test
        void shouldReturnCompletedFuture() {
            CompletableFuture<Void> future = auditService.recordAction(
                    "Wallet", walletId, "WITHDRAWAL",
                    "200.00", "100.00", userId, "10.0.0.1"
            );

            // The CompletableFuture should be immediately completed
            // (the @Async proxy completes it after the method executes)
            assertThat(future).isCompleted();
        }

        @Test
        void shouldNotBlockCallingThread() {
            // Simulates the non-blocking nature: the call returns immediately
            long start = System.nanoTime();

            CompletableFuture<Void> future = auditService.recordAction(
                    "Wallet", walletId, "QUICK_CHECK",
                    "a", "b", userId
            );

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            // The method itself should be near-instant (no I/O in the mock)
            assertThat(elapsedMs).isLessThan(1000);
            assertThat(future).isCompleted();
        }
    }

    // ==================== AuditLog Entity ====================

    @Nested
    class AuditLogDataIntegrity {

        @Test
        void shouldBuildCorrectlyWithBuilder() {
            UUID id = UUID.randomUUID();
            AuditLog log = AuditLog.builder()
                    .entityType("FeeConfiguration")
                    .entityId(id)
                    .action("UPDATE")
                    .oldValue("1.0000")
                    .newValue("0.5000")
                    .performedBy(userId)
                    .ipAddress("127.0.0.1")
                    .build();

            assertThat(log.getEntityType()).isEqualTo("FeeConfiguration");
            assertThat(log.getEntityId()).isEqualTo(id);
            assertThat(log.getAction()).isEqualTo("UPDATE");
            assertThat(log.getOldValue()).isEqualTo("1.0000");
            assertThat(log.getNewValue()).isEqualTo("0.5000");
            assertThat(log.getPerformedBy()).isEqualTo(userId);
            assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
        }
    }
}
