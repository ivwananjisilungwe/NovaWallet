package com.novawallet.novawallet_api.token.service;

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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DEC-02 / NEW-AUTH-01: the revocation store must be durable, idempotent and
 * keyed only by {@code jti}.
 */
@SpringBootTest
@ActiveProfiles("test")
class AccessTokenRevocationServiceTest {

    @Autowired
    private AccessTokenRevocationService revocationService;

    @Autowired
    private RevokedAccessTokenRepository repository;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .email("revocation-" + UUID.randomUUID() + "@test.com")
                .passwordHash("encoded")
                .firstName("Revocation")
                .lastName("Test")
                .phone("+26097" + (System.nanoTime() % 1000000))
                .role(Role.USER)
                .emailVerified(true)
                .build());
        userId = user.getId();
    }

    @Test
    @DisplayName("A fresh jti is not revoked")
    void unknownTokenIsNotRevoked() {
        assertThat(revocationService.isRevoked(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("revoke() persists the jti and isRevoked() then returns true")
    void revokeMakesTokenRevoked() {
        UUID jti = UUID.randomUUID();

        boolean inserted = revocationService.revoke(jti, userId, Instant.now().plus(15, ChronoUnit.MINUTES));

        assertThat(inserted).isTrue();
        assertThat(revocationService.isRevoked(jti)).isTrue();
        assertThat(repository.findById(jti)).isPresent();
    }

    @Test
    @DisplayName("Only the jti is stored — never a bearer token")
    void onlyIdentifierIsStored() {
        UUID jti = UUID.randomUUID();
        revocationService.revoke(jti, userId, Instant.now().plus(15, ChronoUnit.MINUTES));

        var row = repository.findById(jti).orElseThrow();
        assertThat(row.getJti()).isEqualTo(jti);
        assertThat(row.getUserId()).isEqualTo(userId);
        assertThat(row.getRevokedAt()).isNotNull();
        assertThat(row.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("Duplicate revocation is idempotent and does not throw")
    void duplicateRevocationIsIdempotent() {
        UUID jti = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        assertThat(revocationService.revoke(jti, userId, expiresAt)).isTrue();
        assertThat(revocationService.revoke(jti, userId, expiresAt)).isFalse();
        assertThat(revocationService.revoke(jti, userId, expiresAt)).isFalse();

        assertThat(repository.findAll().stream().filter(r -> r.getJti().equals(jti)).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Revocations for different tokens and users are independent")
    void revocationsAreIndependent() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        revocationService.revoke(first, userId, Instant.now().plus(15, ChronoUnit.MINUTES));

        assertThat(revocationService.isRevoked(first)).isTrue();
        assertThat(revocationService.isRevoked(second)).isFalse();
    }

    @Test
    @DisplayName("Expiry is persisted in UTC and round-trips within a second")
    void expiryRoundTrips() {
        UUID jti = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);

        revocationService.revoke(jti, userId, expiresAt);

        var row = repository.findById(jti).orElseThrow();
        assertThat(row.getExpiresAt().toInstant(ZoneOffset.UTC)).isEqualTo(expiresAt);
    }
}
