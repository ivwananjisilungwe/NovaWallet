package com.novawallet.novawallet_api.admin.service;

import com.novawallet.novawallet_api.admin.dto.UserSummaryResponse;
import com.novawallet.novawallet_api.audit.service.AuditService;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
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
}
