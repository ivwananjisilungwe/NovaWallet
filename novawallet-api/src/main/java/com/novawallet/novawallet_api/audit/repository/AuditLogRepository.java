package com.novawallet.novawallet_api.audit.repository;

import com.novawallet.novawallet_api.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByPerformedBy(UUID performedBy, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndCreatedAtBetween(String entityType, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<AuditLog> findByActionAndCreatedAtBetween(String action, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<AuditLog> findByPerformedByAndCreatedAtBetween(UUID performedBy, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
