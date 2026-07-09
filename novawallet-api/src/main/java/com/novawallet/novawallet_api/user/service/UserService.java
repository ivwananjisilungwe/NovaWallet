package com.novawallet.novawallet_api.user.service;

import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.user.dto.request.UpdateProfileRequest;
import com.novawallet.novawallet_api.user.dto.response.UserProfileResponse;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = findUserById(userId);
        return UserProfileResponse.from(user);
    }

    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }

        userRepository.save(user);
        log.info("Profile updated for user: {}", userId);

        return UserProfileResponse.from(user);
    }

    @Transactional(readOnly = true)
    public User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
