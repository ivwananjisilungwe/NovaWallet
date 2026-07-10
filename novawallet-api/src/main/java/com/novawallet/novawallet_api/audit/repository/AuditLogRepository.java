package com.novawallet.novawallet_api.audit.repository;

import com.novawallet.novawallet_api.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
