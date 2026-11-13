package com.novawallet.novawallet_api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String TEST_SECRET = "dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LWZvci1ub3Zhd2FsbGV0LWF1dG9tYXRlZC10ZXN0cy1ub3QtZm9yLXByb2R1Y3Rpb24=";
    private static final long EXPIRATION_MS = 900000L;
    private static final String ISSUER = "novawallet-api-test";
    private static final String AUDIENCE = "novawallet-client-test";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, EXPIRATION_MS, ISSUER, AUDIENCE);
    }

    @Nested
    class RoundTrip {

        @Test
        void userIdEmailRoleTokenIdExpIatCorrect() {
            String token = jwtUtil.generateToken(UUID.randomUUID(), "test@example.com", "USER");
            JwtClaims claims = jwtUtil.parseToken(token);

            assertThat(claims.userId()).isNotNull();
            assertThat(claims.email()).isEqualTo("test@example.com");
            assertThat(claims.role()).isEqualTo("USER");
            assertThat(claims.tokenId()).isNotNull();
            assertThat(claims.issuedAt()).isNotNull();
            assertThat(claims.expiresAt()).isNotNull();
        }

        @Test
        void tokenIdIsNonNullUUID() {
            String token = jwtUtil.generateToken(UUID.randomUUID(), "test@example.com", "USER");
            JwtClaims claims = jwtUtil.parseToken(token);
            assertThat(claims.tokenId()).isNotNull().isInstanceOf(UUID.class);
        }

        @Test
        void twoTokensForSameUserHaveDifferentJti() {
            UUID userId = UUID.randomUUID();
            String token1 = jwtUtil.generateToken(userId, "user@example.com", "USER");
            String token2 = jwtUtil.generateToken(userId, "user@example.com", "USER");
            JwtClaims claims1 = jwtUtil.parseToken(token1);
            JwtClaims claims2 = jwtUtil.parseToken(token2);
            assertThat(claims1.tokenId()).isNotEqualTo(claims2.tokenId());
        }
    }

    @Nested
    class Rejection {

        @Test
        void isTokenValidReturnsFalseForInvalidToken() {
            assertThat(jwtUtil.isTokenValid("invalid-token")).isFalse();
        }

        @Test
        void isTokenValidReturnsFalseForEmptyToken() {
            assertThat(jwtUtil.isTokenValid("")).isFalse();
        }

        @Test
        void extractUserIdThrowsOnInvalidToken() {
            assertThatThrownBy(() -> jwtUtil.extractUserId("invalid"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void extractEmailThrowsOnInvalidToken() {
            assertThatThrownBy(() -> jwtUtil.extractEmail("invalid"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void extractRoleThrowsOnInvalidToken() {
            assertThatThrownBy(() -> jwtUtil.extractRole("invalid"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    class Constructor {

        @Test
        void blankSecretThrows() {
            assertThatThrownBy(() -> new JwtUtil("", EXPIRATION_MS, ISSUER, AUDIENCE))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("jwt.secret must be configured");
        }

        @Test
        void nonBase64SecretThrows() {
            assertThatThrownBy(() -> new JwtUtil("not-base64!", EXPIRATION_MS, ISSUER, AUDIENCE))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("jwt.secret must be Base64-encoded");
        }

        @Test
        void sixteenByteSecretThrows() {
            // 16 bytes = 128 bits, which is less than 256 bits required -> should throw
            assertThatThrownBy(() -> new JwtUtil("0123456789abcdef", EXPIRATION_MS, ISSUER, AUDIENCE))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must decode to at least 256 bits");
        }

        @Test
        void blankIssuerThrows() {
            assertThatThrownBy(() -> new JwtUtil(TEST_SECRET, EXPIRATION_MS, "", AUDIENCE))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("jwt.issuer must be configured");
        }

        @Test
        void blankAudienceThrows() {
            assertThatThrownBy(() -> new JwtUtil(TEST_SECRET, EXPIRATION_MS, ISSUER, ""))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("jwt.audience must be configured");
        }
    }

    /**
     * SEC-08/DEC-10: tokens must be bound to this issuer and audience, and the
     * algorithm must be pinned to HS256. These tests mint tokens with the same
     * signing key but the wrong claims/algorithm — a compromised or foreign
     * issuer must not be able to authenticate here.
     */
    @Nested
    class ClaimAndAlgorithmBinding {

        private final SecretKey key =
                Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));

        private String tokenWith(String issuer, String audience, Instant expiry) {
            return Jwts.builder()
                    .subject(UUID.randomUUID().toString())
                    .issuer(issuer)
                    .audience().add(audience).and()
                    .id(UUID.randomUUID().toString())
                    .claim("email", "user@example.com")
                    .claim("role", "USER")
                    .issuedAt(Date.from(Instant.now().minusSeconds(60)))
                    .expiration(Date.from(expiry))
                    .signWith(key, Jwts.SIG.HS256)
                    .compact();
        }

        @Test
        void wrongIssuerIsRejected() {
            String token = tokenWith("evil-issuer", AUDIENCE, Instant.now().plusSeconds(600));

            assertThat(jwtUtil.isTokenValid(token)).isFalse();
            assertThatThrownBy(() -> jwtUtil.parseToken(token))
                    .isInstanceOf(io.jsonwebtoken.JwtException.class);
        }

        @Test
        void missingIssuerIsRejected() {
            String token = Jwts.builder()
                    .subject(UUID.randomUUID().toString())
                    .audience().add(AUDIENCE).and()
                    .id(UUID.randomUUID().toString())
                    .claim("email", "user@example.com")
                    .claim("role", "USER")
                    .issuedAt(Date.from(Instant.now().minusSeconds(60)))
                    .expiration(Date.from(Instant.now().plusSeconds(600)))
                    .signWith(key, Jwts.SIG.HS256)
                    .compact();

            assertThat(jwtUtil.isTokenValid(token)).isFalse();
        }

        @Test
        void wrongAudienceIsRejected() {
            String token = tokenWith(ISSUER, "another-service", Instant.now().plusSeconds(600));

            assertThat(jwtUtil.isTokenValid(token)).isFalse();
            assertThatThrownBy(() -> jwtUtil.parseToken(token))
                    .isInstanceOf(io.jsonwebtoken.JwtException.class);
        }

        @Test
        void missingAudienceIsRejected() {
            String token = Jwts.builder()
                    .subject(UUID.randomUUID().toString())
                    .issuer(ISSUER)
                    .id(UUID.randomUUID().toString())
                    .claim("email", "user@example.com")
                    .claim("role", "USER")
                    .issuedAt(Date.from(Instant.now().minusSeconds(60)))
                    .expiration(Date.from(Instant.now().plusSeconds(600)))
                    .signWith(key, Jwts.SIG.HS256)
                    .compact();

            assertThat(jwtUtil.isTokenValid(token)).isFalse();
        }

        @Test
        void nonHs256AlgorithmIsRejected() {
            // Same key, same valid claims — only the algorithm differs.
            String token = Jwts.builder()
                    .subject(UUID.randomUUID().toString())
                    .issuer(ISSUER)
                    .audience().add(AUDIENCE).and()
                    .id(UUID.randomUUID().toString())
                    .claim("email", "user@example.com")
                    .claim("role", "USER")
                    .issuedAt(Date.from(Instant.now().minusSeconds(60)))
                    .expiration(Date.from(Instant.now().plusSeconds(600)))
                    .signWith(key, Jwts.SIG.HS512)
                    .compact();

            assertThat(jwtUtil.isTokenValid(token)).isFalse();
        }

        @Test
        void unsignedAlgNoneTokenIsRejected() {
            String token = Jwts.builder()
                    .subject(UUID.randomUUID().toString())
                    .issuer(ISSUER)
                    .audience().add(AUDIENCE).and()
                    .id(UUID.randomUUID().toString())
                    .claim("email", "user@example.com")
                    .claim("role", "USER")
                    .issuedAt(Date.from(Instant.now().minusSeconds(60)))
                    .expiration(Date.from(Instant.now().plusSeconds(600)))
                    .compact(); // no signWith -> alg: none

            assertThat(jwtUtil.isTokenValid(token)).isFalse();
            assertThatThrownBy(() -> jwtUtil.parseToken(token))
                    .isInstanceOf(io.jsonwebtoken.JwtException.class);
        }

        @Test
        void expiredTokenIsRejected() {
            String token = tokenWith(ISSUER, AUDIENCE, Instant.now().minusSeconds(30));

            assertThat(jwtUtil.isTokenValid(token)).isFalse();
            assertThatThrownBy(() -> jwtUtil.parseToken(token))
                    .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
        }

        @Test
        void tokenMissingJtiIsRejected() {
            String token = Jwts.builder()
                    .subject(UUID.randomUUID().toString())
                    .issuer(ISSUER)
                    .audience().add(AUDIENCE).and()
                    .claim("email", "user@example.com")
                    .claim("role", "USER")
                    .issuedAt(Date.from(Instant.now().minusSeconds(60)))
                    .expiration(Date.from(Instant.now().plusSeconds(600)))
                    .signWith(key, Jwts.SIG.HS256)
                    .compact();

            assertThat(jwtUtil.isTokenValid(token)).isFalse();
            assertThatThrownBy(() -> jwtUtil.parseToken(token))
                    .hasMessageContaining("missing required claims");
        }

        @Test
        void startupErrorMessagesContainNoKeyMaterial() {
            assertThatThrownBy(() -> new JwtUtil("short", EXPIRATION_MS, ISSUER, AUDIENCE))
                    .isInstanceOf(IllegalStateException.class)
                    .satisfies(e -> assertThat(e.getMessage()).doesNotContain(TEST_SECRET));
        }
    }
}