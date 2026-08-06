package com.novawallet.novawallet_api.audit.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Maps to the {@code audit_logs} table. Append-only audit trail for all balance changes.
 *
 * <p>This is an immutable, append-only log. No update or delete operations are permitted.
 * Each entry records the entity type, entity ID, action performed, old and new values,
 * the admin/user who performed the action, and the IP address. Writes are asynchronous
 * and non-blocking via {@code @Async}.</p>
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuditLog() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ==================== Builder ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String entityType;
        private UUID entityId;
        private String action;
        private String oldValue;
        private String newValue;
        private UUID performedBy;
        private String ipAddress;

        public Builder entityType(String entityType) { this.entityType = entityType; return this; }
        public Builder entityId(UUID entityId) { this.entityId = entityId; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder oldValue(String oldValue) { this.oldValue = oldValue; return this; }
        public Builder newValue(String newValue) { this.newValue = newValue; return this; }
        public Builder performedBy(UUID performedBy) { this.performedBy = performedBy; return this; }
        public Builder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }

        public AuditLog build() {
            AuditLog log = new AuditLog();
            log.entityType = entityType;
            log.entityId = entityId;
            log.action = action;
            log.oldValue = oldValue;
            log.newValue = newValue;
            log.performedBy = performedBy;
            log.ipAddress = ipAddress;
            return log;
        }
    }

    // ==================== Getters ====================

    public UUID getId() { return id; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getAction() { return action; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public UUID getPerformedBy() { return performedBy; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
