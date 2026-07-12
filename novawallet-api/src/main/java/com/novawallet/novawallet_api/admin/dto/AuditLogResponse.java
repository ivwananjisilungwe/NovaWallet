package com.novawallet.novawallet_api.admin.dto;

import com.novawallet.novawallet_api.audit.entity.AuditLog;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Audit log entry for admin views")
public record AuditLogResponse(
        @Schema(description = "Audit log ID", example = "550e8400-e29b-41d4-a716-446655440003")
        String id,

        @Schema(description = "Entity type that was modified", example = "Wallet")
        String entityType,

        @Schema(description = "Entity ID that was modified")
        String entityId,

        @Schema(description = "Action performed", example = "WALLET_FREEZE")
        String action,

        @Schema(description = "Old value before change")
        String oldValue,

        @Schema(description = "New value after change")
        String newValue,

        @Schema(description = "Admin who performed the action")
        String performedBy,

        @Schema(description = "Timestamp of the action")
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId().toString(),
                log.getEntityType(),
                log.getEntityId() != null ? log.getEntityId().toString() : null,
                log.getAction(),
                log.getOldValue(),
                log.getNewValue(),
                log.getPerformedBy() != null ? log.getPerformedBy().toString() : null,
                log.getCreatedAt()
        );
    }
}
