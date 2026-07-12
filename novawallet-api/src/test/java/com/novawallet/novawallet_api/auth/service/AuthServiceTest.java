package com.novawallet.novawallet_api.auth.service;

import com.novawallet.novawallet_api.auth.dto.request.*;
import com.novawallet.novawallet_api.auth.dto.response.AuthResponse;
import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.exception.DuplicateResourceException;
import com.novawallet.novawallet_api.exception.UnauthorizedException;
import com.novawallet.novawallet_api.notification.MailService;
import com.novawallet.novawallet_api.notification.entity.Notification;
import com.novawallet.novawallet_api.notification.repository.NotificationRepository;
import com.novawallet.novawallet_api.notification.repository.NotificationAttemptRepository;
import com.novawallet.novawallet_api.notification.service.NotificationService;
import com.novawallet.novawallet_api.notification.service.SmsService;
import com.novawallet.novawallet_api.security.JwtUtil;
import com.novawallet.novawallet_api.token.entity.RefreshToken;
import com.novawallet.novawallet_api.token.repository.RefreshTokenRepository;
import com.novawallet.novawallet_api.token.service.TokenService;
import com.novawallet.novawallet_api.user.entity.Role;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private MailService mailService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationAttemptRepository notificationAttemptRepository;
    @Mock
    private SmsService smsService;

    private PasswordEncoder passwordEncoder;
    private TokenService tokenService;
    private AuthService authService;
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<User> userCaptor;
    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        // Manual inject for TokenService
        tokenService = new TokenService(refreshTokenRepository);
        // We need to mock the hash/save behavior. Using spy.
        tokenService = spy(tokenService);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        when(notificationAttemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService = new NotificationService(
                notificationRepository, notificationAttemptRepository, mailService, smsService);

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtUtil,
                tokenService,
                mailService,
                notificationService
        );

        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("test-access-token");
        when(jwtUtil.getExpirationMs()).thenReturn(900000L);
    }

    // ==================== Register ====================

    @Nested
    class Register {

        @Test
        void shouldRegisterUserSuccessfully() {
            RegisterRequest request = new RegisterRequest(
                    "John", "Doe", "john@example.com",
                    "+260971234567", "SecurePass@123"
            );

            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userRepository.existsByPhone("+260971234567")).thenReturn(false);

            User savedUser = User.builder()
                    .id(UUID.randomUUID())
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .phone("+260971234567")
                    .passwordHash(passwordEncoder.encode("SecurePass@123"))
                    .role(Role.USER)
                    .verificationToken("some-token")
                    .build();

            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            doReturn("test-refresh-token").when(tokenService).createRefreshToken(any(User.class));

            AuthResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("test-access-token");
            assertThat(response.user().firstName()).isEqualTo("John");
            assertThat(response.user().email()).isEqualTo("john@example.com");

            verify(userRepository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getPasswordHash()).isNotEqualTo("SecurePass@123");
            assertThat(capturedUser.getVerificationToken()).isNotBlank();
        }

        @Test
        void shouldThrowWhenEmailAlreadyExists() {
            RegisterRequest request = new RegisterRequest(
                    "John", "Doe", "existing@example.com",
                    "+260971234567", "SecurePass@123"
            );

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Email already registered");
        }

        @Test
        void shouldThrowWhenPhoneAlreadyExists() {
            RegisterRequest request = new RegisterRequest(
                    "John", "Doe", "john@example.com",
                    "+260971234567", "SecurePass@123"
            );

            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userRepository.existsByPhone("+260971234567")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Phone number already registered");
        }
    }

    // ==================== Login ====================

    @Nested
    class Login {

        @Test
        void shouldLoginSuccessfully() {
            String rawPassword = "SecurePass@123";
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .phone("+260971234567")
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .role(Role.USER)
                    .build();

            when(userRepository.findByEmail("john@example.com"))
                    .thenReturn(Optional.of(user));

            doReturn("test-refresh-token").when(tokenService).createRefreshToken(any(User.class));

            LoginRequest request = new LoginRequest("john@example.com", rawPassword);
            AuthResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.user().email()).isEqualTo("john@example.com");
        }

        @Test
        void shouldThrowWhenEmailNotFound() {
            when(userRepository.findByEmail("unknown@example.com"))
                    .thenReturn(Optional.empty());

            LoginRequest request = new LoginRequest("unknown@example.com", "password123");

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        void shouldThrowWhenPasswordIncorrect() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email("john@example.com")
                    .passwordHash(passwordEncoder.encode("CorrectPass123"))
                    .build();

            when(userRepository.findByEmail("john@example.com"))
                    .thenReturn(Optional.of(user));

            LoginRequest request = new LoginRequest("john@example.com", "WrongPass123");

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }
    }

    // ==================== Set PIN ====================

    @Nested
    class SetPin {

        @Test
        void shouldSetPinSuccessfully() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .email("john@example.com")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            authService.setPin(userId, "1234");

            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getPinHash()).isNotEqualTo("1234");
            assertThat(saved.getPinAttempts()).isZero();
        }

        @Test
        void shouldThrowWhenPinIsLocked() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .pinLockedUntil(LocalDateTime.now().plusHours(1))
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.setPin(userId, "1234"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("PIN is locked");
        }
    }

    // ==================== Email Verification ====================

    @Nested
    class VerifyEmail {

        @Test
        void shouldVerifyEmailSuccessfully() {
            String token = "verification-token-123";
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email("john@example.com")
                    .emailVerified(false)
                    .verificationToken(token)
                    .build();

            when(userRepository.findByVerificationToken(token))
                    .thenReturn(Optional.of(user));

            authService.verifyEmail(token);

            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.isEmailVerified()).isTrue();
            assertThat(saved.getVerificationToken()).isNull();
        }

        @Test
        void shouldThrowWhenTokenInvalid() {
            when(userRepository.findByVerificationToken("invalid-token"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail("invalid-token"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void shouldThrowWhenAlreadyVerified() {
            String token = "verified-token";
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .emailVerified(true)
                    .verificationToken(token)
                    .build();

            when(userRepository.findByVerificationToken(token))
                    .thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyEmail(token))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already verified");
        }
    }

    // ==================== Forgot Password ====================

    @Nested
    class ForgotPassword {

        @Test
        void shouldGenerateResetToken() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .email("john@example.com")
                    .build();

            when(userRepository.findByEmail("john@example.com"))
                    .thenReturn(Optional.of(user));

            authService.forgotPassword(new ForgotPasswordRequest("john@example.com"));

            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getPasswordResetToken()).isNotBlank();
            assertThat(saved.getPasswordResetExpiresAt()).isNotNull();
        }

        @Test
        void shouldNotThrowWhenEmailNotFound() {
            when(userRepository.findByEmail("unknown@example.com"))
                    .thenReturn(Optional.empty());

            authService.forgotPassword(new ForgotPasswordRequest("unknown@example.com"));

            verify(userRepository, never()).save(any());
        }
    }

    // ==================== Reset Password ====================

    @Nested
    class ResetPassword {

        @Test
        void shouldResetPasswordSuccessfully() {
            String token = "reset-token-123";
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .email("john@example.com")
                    .passwordHash(passwordEncoder.encode("OldPass123"))
                    .passwordResetToken(token)
                    .passwordResetExpiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            when(userRepository.findByPasswordResetToken(token))
                    .thenReturn(Optional.of(user));

            authService.resetPassword(new ResetPasswordRequest(token, "NewPass456"));

            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getPasswordHash()).isNotEqualTo("OldPass123");
            assertThat(saved.getPasswordResetToken()).isNull();
            assertThat(saved.getPasswordResetExpiresAt()).isNull();

            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }

        @Test
        void shouldThrowWhenTokenExpired() {
            String token = "expired-token";
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .passwordResetToken(token)
                    .passwordResetExpiresAt(LocalDateTime.now().minusMinutes(5))
                    .build();

            when(userRepository.findByPasswordResetToken(token))
                    .thenReturn(Optional.of(user));

            assertThatThrownBy(() ->
                    authService.resetPassword(new ResetPasswordRequest(token, "NewPass456"))
            ).isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expired");
        }
    }

    // ==================== Logout ====================

    @Nested
    class Logout {

        @Test
        void shouldRevokeAllTokens() {
            UUID userId = UUID.randomUUID();

            authService.logout(userId);

            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }
    }
}
