package com.novawallet.novawallet_api.audit.service;

import com.novawallet.novawallet_api.audit.entity.AuditLog;
import com.novawallet.novawallet_api.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> recordAction(
            String entityType,
            UUID entityId,
            String action,
            String oldValue,
            String newValue,
            UUID performedBy,
            String ipAddress
    ) {
        AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .performedBy(performedBy)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(auditLog);

        log.debug("Audit log recorded: {} {} (id={})", action, entityType, entityId);

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Convenience overload without IP address (when IP is not available).
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> recordAction(
            String entityType,
            UUID entityId,
            String action,
            String oldValue,
            String newValue,
            UUID performedBy
    ) {
        return recordAction(entityType, entityId, action, oldValue, newValue, performedBy, null);
    }
}
