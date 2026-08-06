package com.novawallet.novawallet_api.user.service;

import com.novawallet.novawallet_api.exception.UnauthorizedException;
import com.novawallet.novawallet_api.user.dto.request.ChangePasswordRequest;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    // ==================== Change password ====================

    @Nested
    class ChangePassword {

        @Test
        void shouldUpdatePasswordHashWhenCurrentPasswordIsCorrect() {
            UUID userId = UUID.randomUUID();
            String oldHash = passwordEncoder.encode("OldPass@123");
            User user = User.builder()
                    .id(userId)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .phone("+260971234567")
                    .passwordHash(oldHash)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            ChangePasswordRequest request = new ChangePasswordRequest("OldPass@123", "NewPass@456");

            userService.changePassword(userId, request);

            assertThat(user.getPasswordHash()).isNotEqualTo(oldHash);
            assertThat(passwordEncoder.matches("NewPass@456", user.getPasswordHash())).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        void shouldThrowUnauthorizedExceptionWhenCurrentPasswordIsIncorrect() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .phone("+260971234567")
                    .passwordHash(passwordEncoder.encode("OldPass@123"))
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            ChangePasswordRequest request = new ChangePasswordRequest("WrongPass@999", "NewPass@456");

            assertThatThrownBy(() -> userService.changePassword(userId, request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Current password is incorrect");
            verify(userRepository, never()).save(any(User.class));
        }
    }
}