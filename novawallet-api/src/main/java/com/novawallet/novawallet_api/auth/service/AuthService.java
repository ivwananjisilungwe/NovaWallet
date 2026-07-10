package com.novawallet.novawallet_api.auth.service;

import com.novawallet.novawallet_api.auth.dto.request.*;
import com.novawallet.novawallet_api.auth.dto.response.AuthResponse;
import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.exception.DuplicateResourceException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.exception.UnauthorizedException;
import com.novawallet.novawallet_api.notification.MailService;
import com.novawallet.novawallet_api.security.JwtUtil;
import com.novawallet.novawallet_api.token.entity.RefreshToken;
import com.novawallet.novawallet_api.token.service.TokenService;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_PIN_ATTEMPTS = 3;
    private static final int PIN_LOCK_MINUTES = 15;
    private static final int PASSWORD_RESET_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final MailService mailService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            TokenService tokenService,
            MailService mailService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
        this.mailService = mailService;
    }

    // ==================== Register ====================

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new DuplicateResourceException("Phone number already registered: " + request.phone());
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email().toLowerCase().trim())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .verificationToken(tokenService.generateSecureToken())
                .build();

        user = userRepository.save(user);

        log.info("User registered: {} (id={})", user.getEmail(), user.getId());

        mailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), user.getVerificationToken());

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
        User user = userRepository.findByVerificationToken(token)
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
                    user.setPasswordResetToken(tokenService.generateSecureToken());
                    user.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(PASSWORD_RESET_HOURS));
                    userRepository.save(user);

                    mailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), user.getPasswordResetToken());
                    log.info("Password reset token generated for user: {}", user.getEmail());
                });
    }

    // ==================== Reset Password ====================

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.token())
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
        RefreshToken storedToken = tokenService.validateAndGetRefreshToken(refreshTokenValue);

        // Rotate: revoke old, issue new
        tokenService.revokeToken(storedToken);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newRefreshToken = tokenService.createRefreshToken(user);

        return buildAuthResponse(user, newRefreshToken);
    }

    // ==================== Logout ====================

    public void logout(UUID userId) {
        tokenService.revokeAllUserTokens(userId);
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
