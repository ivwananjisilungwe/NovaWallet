package com.novawallet.novawallet_api.token.service;

import com.novawallet.novawallet_api.exception.UnauthorizedException;
import com.novawallet.novawallet_api.token.entity.RefreshToken;
import com.novawallet.novawallet_api.token.repository.RefreshTokenRepository;
import com.novawallet.novawallet_api.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
public class TokenService {

    private static final int REFRESH_TOKEN_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;

    public TokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String createRefreshToken(User user) {
        String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_DAYS))
                .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public RefreshToken validateAndGetRefreshToken(String refreshTokenValue) {
        String tokenHash = hashToken(refreshTokenValue);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(storedToken.getUserId());
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        return storedToken;
    }

    public void revokeToken(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    public String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    public String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
