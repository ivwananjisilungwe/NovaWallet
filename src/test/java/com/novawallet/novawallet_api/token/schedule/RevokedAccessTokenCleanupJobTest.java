package com.novawallet.novawallet_api.token.schedule;

import com.novawallet.novawallet_api.token.entity.RevokedAccessToken;
import com.novawallet.novawallet_api.token.repository.RevokedAccessTokenRepository;
import com.novawallet.novawallet_api.user.entity.Role;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DEC-02: cleanup must remove rows whose token would already have failed
 * expiry validation, and must never remove a row for a live token.
 */
@SpringBootTest
@ActiveProfiles("test")
class RevokedAccessTokenCleanupJobTest {

    @Autowired
    private RevokedAccessTokenCleanupJob cleanupJob;

    @Autowired
    private RevokedAccessTokenRepository repository;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .email("cleanup-" + UUID.randomUUID() + "@test.com")
                .passwordHash("encoded")
                .firstName("Cleanup")
                .lastName("Test")
                .phone("+26097" + (System.nanoTime() % 1000000))
                .role(Role.USER)
                .emailVerified(true)
                .build());
        userId = user.getId();
    }

    private UUID saveRow(LocalDateTime expiresAt) {
        UUID jti = UUID.randomUUID();
        repository.saveAndFlush(RevokedAccessToken.builder()
                .jti(jti)
                .userId(userId)
                .revokedAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(1))
                .expiresAt(expiresAt)
                .build());
        return jti;
    }

    @Test
    @DisplayName("Expired revocation rows are removed")
    void removesExpiredRows() {
        UUID expired = saveRow(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));

        cleanupJob.cleanupExpiredTokens();

        assertThat(repository.findById(expired)).isEmpty();
    }

    @Test
    @DisplayName("Live revocation rows are retained")
    void retainsLiveRows() {
        UUID live = saveRow(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));

        cleanupJob.cleanupExpiredTokens();

        assertThat(repository.findById(live)).isPresent();
    }

    @Test
    @DisplayName("Cleanup is safe to run twice")
    void cleanupIsIdempotent() {
        UUID expired = saveRow(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));

        cleanupJob.cleanupExpiredTokens();
        cleanupJob.cleanupExpiredTokens();

        assertThat(repository.findById(expired)).isEmpty();
    }
}
