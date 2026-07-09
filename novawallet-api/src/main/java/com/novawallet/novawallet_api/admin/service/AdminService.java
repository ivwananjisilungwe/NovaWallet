package com.novawallet.novawallet_api.admin.service;

import com.novawallet.novawallet_api.admin.dto.UserSummaryResponse;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return UserSummaryResponse.from(user);
    }

    public void toggleUserDeletedStatus(UUID userId) {
        User user = userRepository.findByIdIncludingDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.isDeleted()) {
            user.setDeleted(false);
            user.setDeletedAt(null);
            log.info("Admin restored user: {}", userId);
        } else {
            user.setDeleted(true);
            user.setDeletedAt(java.time.LocalDateTime.now());
            log.info("Admin soft-deleted user: {}", userId);
        }

        userRepository.save(user);
    }
}
