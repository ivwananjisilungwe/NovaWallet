package com.novawallet.novawallet_api.admin.service;

import com.novawallet.novawallet_api.admin.dto.AuditLogResponse;
import com.novawallet.novawallet_api.admin.dto.TransactionSearchResponse;
import com.novawallet.novawallet_api.admin.dto.UserSummaryResponse;
import com.novawallet.novawallet_api.audit.entity.AuditLog;
import com.novawallet.novawallet_api.audit.repository.AuditLogRepository;
import com.novawallet.novawallet_api.audit.service.AuditService;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminService(UserRepository userRepository, AuditService auditService,
                        TransactionRepository transactionRepository,
                        AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return UserSummaryResponse.from(user);
    }

    public void toggleUserDeletedStatus(UUID userId, UUID adminId) {
        User user = userRepository.findByIdIncludingDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String oldValue = user.isDeleted() ? "DELETED" : "ACTIVE";
        String newValue;

        if (user.isDeleted()) {
            user.setDeleted(false);
            user.setDeletedAt(null);
            newValue = "ACTIVE";
            log.info("Admin {} restored user: {}", adminId, userId);
        } else {
            user.setDeleted(true);
            user.setDeletedAt(java.time.LocalDateTime.now());
            newValue = "DELETED";
            log.info("Admin {} soft-deleted user: {}", adminId, userId);
        }

        userRepository.save(user);
        auditService.recordAction("User", userId, "USER_TOGGLE_DELETE", oldValue, newValue, adminId);
    }

    @Transactional(readOnly = true)
    public Page<TransactionSearchResponse> searchTransactions(String reference, String type,
                                                               String status, LocalDate dateFrom,
                                                               LocalDate dateTo, String walletId,
                                                               Pageable pageable) {
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (reference != null && !reference.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("reference")),
                        "%" + reference.toLowerCase() + "%"));
            }
            if (type != null && !type.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("type"), TransactionType.valueOf(type.toUpperCase())));
                } catch (IllegalArgumentException ignored) {}
            }
            if (status != null && !status.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("status"), TransactionStatus.valueOf(status.toUpperCase())));
                } catch (IllegalArgumentException ignored) {}
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo.atTime(LocalTime.MAX)));
            }
            if (walletId != null && !walletId.isBlank()) {
                try {
                    UUID wId = UUID.fromString(walletId);
                    predicates.add(cb.or(
                            cb.equal(root.get("senderWalletId"), wId),
                            cb.equal(root.get("receiverWalletId"), wId)
                    ));
                } catch (IllegalArgumentException ignored) {}
            }

            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec, pageable).map(TransactionSearchResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchAuditLogs(String entityType, String action,
                                                   String performedBy, LocalDate dateFrom,
                                                   LocalDate dateTo, Pageable pageable) {
        if (entityType != null && !entityType.isBlank()
                && dateFrom != null && dateTo != null) {
            return auditLogRepository.findByEntityTypeAndCreatedAtBetween(
                    entityType, dateFrom.atStartOfDay(), dateTo.atTime(LocalTime.MAX), pageable
            ).map(AuditLogResponse::from);
        }
        if (action != null && !action.isBlank()
                && dateFrom != null && dateTo != null) {
            return auditLogRepository.findByActionAndCreatedAtBetween(
                    action, dateFrom.atStartOfDay(), dateTo.atTime(LocalTime.MAX), pageable
            ).map(AuditLogResponse::from);
        }
        if (performedBy != null && !performedBy.isBlank()
                && dateFrom != null && dateTo != null) {
            try {
                UUID pb = UUID.fromString(performedBy);
                return auditLogRepository.findByPerformedByAndCreatedAtBetween(
                        pb, dateFrom.atStartOfDay(), dateTo.atTime(LocalTime.MAX), pageable
                ).map(AuditLogResponse::from);
            } catch (IllegalArgumentException ignored) {}
        }
        if (entityType != null && !entityType.isBlank()) {
            return auditLogRepository.findByEntityType(entityType, pageable)
                    .map(AuditLogResponse::from);
        }
        if (action != null && !action.isBlank()) {
            return auditLogRepository.findByAction(action, pageable)
                    .map(AuditLogResponse::from);
        }
        if (performedBy != null && !performedBy.isBlank()) {
            try {
                UUID pb = UUID.fromString(performedBy);
                return auditLogRepository.findByPerformedBy(pb, pageable)
                        .map(AuditLogResponse::from);
            } catch (IllegalArgumentException ignored) {}
        }
        if (dateFrom != null && dateTo != null) {
            return auditLogRepository.findByCreatedAtBetween(
                    dateFrom.atStartOfDay(), dateTo.atTime(LocalTime.MAX), pageable
            ).map(AuditLogResponse::from);
        }
        return auditLogRepository.findAll(pageable).map(AuditLogResponse::from);
    }
}
