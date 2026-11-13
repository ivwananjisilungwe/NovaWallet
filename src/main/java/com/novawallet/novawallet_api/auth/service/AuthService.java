package com.novawallet.novawallet_api.auth.service;

import com.novawallet.novawallet_api.auth.dto.request.*;
import com.novawallet.novawallet_api.auth.dto.response.AuthResponse;
import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.exception.DuplicateResourceException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.exception.UnauthorizedException;
import com.novawallet.novawallet_api.notification.MailService;
import com.novawallet.novawallet_api.notification.entity.NotificationChannel;
import com.novawallet.novawallet_api.notification.entity.NotificationType;
import com.novawallet.novawallet_api.notification.service.NotificationService;
import com.novawallet.novawallet_api.security.JwtUtil;
import com.novawallet.novawallet_api.token.entity.RefreshToken;
import com.novawallet.novawallet_api.token.service.AccessTokenRevocationService;
import com.novawallet.novawallet_api.token.service.TokenService;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles user registration, authentication, and session management.
 *
 * <p>Registration creates a user with BCrypt-hashed password, generates
 * an email verification token, and issues JWT + refresh token pair.
 * Login validates credentials and returns tokens. Password reset and
 * email verification flows use time-limited tokens. All sensitive
 * operations respect rate limiting and lockout policies.</p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_PIN_ATTEMPTS = 3;
    private static final int PIN_LOCK_MINUTES = 15;
    private static final int PASSWORD_RESET_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final AccessTokenRevocationService accessTokenRevocationService;
    private final MailService mailService;
    private final NotificationService notificationService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            TokenService tokenService,
            AccessTokenRevocationService accessTokenRevocationService,
            MailService mailService,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
        this.accessTokenRevocationService = accessTokenRevocationService;
        this.mailService = mailService;
        this.notificationService = notificationService;
    }

    // ==================== Register ====================

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new DuplicateResourceException("Phone number already registered: " + request.phone());
        }

        // Only the SHA-256 hash of the verification token is stored. The raw
        // token exists solely in the delivered email (SEC-03).
        String rawVerificationToken = tokenService.generateSecureToken();

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email().toLowerCase().trim())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .verificationToken(tokenService.hashToken(rawVerificationToken))
                .build();

        user = userRepository.save(user);

        log.info("User registered: {} (id={})", user.getEmail(), user.getId());

        mailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), rawVerificationToken);
        notificationService.recordSent(user.getId(), user.getEmail(), NotificationChannel.EMAIL,
                NotificationType.EMAIL_VERIFICATION, "NovaWallet — Verify your email address",
                "Please verify your email address.");

        String refreshToken = tokenService.createRefreshToken(user);

        return buildAuthResponse(user, refreshToken);
    }

    // ==================== Login ====================

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Failed login attempt for email: {}", request.email());
            throw new UnauthorizedException("Invalid email or password");
        }

        String refreshToken = tokenService.createRefreshToken(user);

        log.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(user, refreshToken);
    }

    // ==================== Set PIN ====================

    public void setPin(UUID userId, String pin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (isPinLocked(user)) {
            throw new BadRequestException("PIN is locked until " + user.getPinLockedUntil());
        }

        user.setPinHash(passwordEncoder.encode(pin));
        user.setPinAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);

        log.info("PIN set for user: {}", userId);
    }

    public void changePin(UUID userId, String currentPin, String newPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (isPinLocked(user)) {
            throw new BadRequestException("PIN is locked until " + user.getPinLockedUntil());
        }

        if (user.getPinHash() == null) {
            throw new BadRequestException("No PIN set — use set PIN first");
        }

        if (!passwordEncoder.matches(currentPin, user.getPinHash())) {
            throw new BadRequestException("Current PIN is incorrect");
        }

        user.setPinHash(passwordEncoder.encode(newPin));
        user.setPinAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);

        log.info("PIN changed for user: {}", userId);
    }

    // ==================== Verify PIN (with rate limiting) ====================

    public boolean verifyPin(UUID userId, String pin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (isPinLocked(user)) {
            throw new BadRequestException("PIN is locked until " + user.getPinLockedUntil());
        }

        if (user.getPinHash() == null) {
            throw new BadRequestException("PIN has not been set");
        }

        if (passwordEncoder.matches(pin, user.getPinHash())) {
            user.setPinAttempts(0);
            user.setPinLockedUntil(null);
            userRepository.save(user);
            log.info("PIN verified for user: {}", userId);
            return true;
        }

        user.setPinAttempts(user.getPinAttempts() + 1);
        if (user.getPinAttempts() >= MAX_PIN_ATTEMPTS) {
            user.setPinLockedUntil(LocalDateTime.now().plusMinutes(PIN_LOCK_MINUTES));
            log.warn("PIN locked for user: {} after {} failed attempts", userId, user.getPinAttempts());
        }
        userRepository.save(user);
        log.warn("Failed PIN verification for user: {} (attempt {})", userId, user.getPinAttempts());
        throw new BadRequestException("Invalid PIN");
    }

    // ==================== Verify Email ====================

    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(tokenService.hashToken(token))
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email already verified");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
    }

    // ==================== Forgot Password ====================

    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email().toLowerCase().trim())
                .ifPresent(user -> {
                    // Only the hash is persisted; the raw token is emailed and
                    // then discarded (SEC-03).
                    String rawResetToken = tokenService.generateSecureToken();

                    user.setPasswordResetToken(tokenService.hashToken(rawResetToken));
                    user.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(PASSWORD_RESET_HOURS));
                    userRepository.save(user);

                    mailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), rawResetToken);
                    notificationService.recordSent(user.getId(), user.getEmail(), NotificationChannel.EMAIL,
                            NotificationType.PASSWORD_RESET, "NovaWallet — Password Reset Request",
                            "A password reset was requested for your account.");
                    log.info("Password reset token generated for user: {}", user.getEmail());
                });
    }

    // ==================== Reset Password ====================

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(tokenService.hashToken(request.token()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (user.getPasswordResetExpiresAt() == null ||
                user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);

        tokenService.revokeAllUserTokens(user.getId());

        log.info("Password reset completed for user: {}", user.getEmail());
    }

    // ==================== Refresh Token ====================

    public AuthResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken storedToken;
        try {
            storedToken = tokenService.validateAndGetRefreshToken(refreshTokenValue);
        } catch (UnauthorizedException e) {
            if (e.getMessage().equals("Refresh token has been revoked")) {
                String tokenHash = tokenService.hashToken(refreshTokenValue);
                UUID userId = tokenService.findUserIdByTokenHash(tokenHash);
                if (userId != null) {
                    tokenService.revokeAllUserTokens(userId);
                }
            }
            throw e;
        }

        // Rotate: revoke old, issue new
        tokenService.revokeToken(storedToken);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newRefreshToken = tokenService.createRefreshToken(user);

        return buildAuthResponse(user, newRefreshToken);
    }

    // ==================== Logout ====================

    /**
     * Ends the current session: revokes the presented access token
     * immediately (DEC-02) and every refresh token owned by the user.
     *
     * <p>Revoking the refresh tokens is what prevents the session from being
     * resumed; revoking the access token closes the 15-minute window in
     * which the still-valid bearer token could otherwise be replayed.</p>
     *
     * @param userId         authenticated user
     * @param accessTokenId  {@code jti} of the access token used for this request
     * @param accessTokenExpiry natural expiry of that access token
     */
    @Transactional
    public void logout(UUID userId, UUID accessTokenId, Instant accessTokenExpiry) {
        // Both revocation writes participate in this transaction so a failure
        // while revoking refresh tokens rolls back the access-token revocation.
        // The client can then retry with the same still-valid access token.
        accessTokenRevocationService.revokeInCurrentTransaction(accessTokenId, userId, accessTokenExpiry);
        tokenService.revokeAllUserTokensInCurrentTransaction(userId);
        log.info("User logged out: {}", userId);
    }

    // ==================== Private Helpers ====================

    private AuthResponse buildAuthResponse(User user, String refreshToken) {
        String accessToken = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtUtil.getExpirationMs() / 1000,
                new AuthResponse.UserInfo(
                        user.getId().toString(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getRole().name(),
                        user.isEmailVerified(),
                        user.getPinHash() != null
                )
        );
    }

    private boolean isPinLocked(User user) {
        return user.getPinLockedUntil() != null &&
                user.getPinLockedUntil().isAfter(LocalDateTime.now());
    }
}
