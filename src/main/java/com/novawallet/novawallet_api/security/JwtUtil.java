package com.novawallet.novawallet_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Generates and validates JWT access tokens.
 *
 * <p>Security properties enforced here (Phase 1C, SEC-08/DEC-10):</p>
 * <ul>
 *   <li>The signing algorithm is pinned to {@code HS256} on both issuance and
 *       verification. Tokens declaring any other algorithm (including
 *       {@code none}) are rejected, which prevents algorithm-confusion
 *       attacks.</li>
 *   <li>Every token carries a unique {@code jti}. The identifier is the
 *       primary key of the access-token revocation store, so a token can be
 *       invalidated before its natural expiry.</li>
 *   <li>Issuer and audience are required on issuance and mandatory on
 *       verification ({@code iss}/{@code aud}). A token minted for another
 *       service or by another issuer does not authenticate here.</li>
 *   <li>The signing secret must be present, Base64-encoded and strong enough
 *       for HS256 (at least 256 bits). Startup fails fast otherwise.</li>
 * </ul>
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final String issuer;
    private final String audience;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience
    ) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("jwt.secret must be configured");
        }
        if (!StringUtils.hasText(issuer)) {
            throw new IllegalStateException("jwt.issuer must be configured");
        }
        if (!StringUtils.hasText(audience)) {
            throw new IllegalStateException("jwt.audience must be configured");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException e) {
            throw new IllegalStateException("jwt.secret must be Base64-encoded", e);
        }

        try {
            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (WeakKeyException e) {
            throw new IllegalStateException(
                    "jwt.secret must decode to at least 256 bits (32 bytes) for HS256", e);
        }

        this.expirationMs = expirationMs;
        this.issuer = issuer;
        this.audience = audience;
    }

    /**
     * Issues a signed access token with a fresh {@code jti}.
     */
    public String generateToken(UUID userId, String email, String role) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .audience().add(audience).and()
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifies signature, algorithm, issuer, audience, expiry and required
     * claims, returning an immutable view of the payload.
     *
     * @throws JwtException             if the token is invalid, expired or fails a required check
     * @throws IllegalArgumentException if the subject or {@code jti} is not a UUID
     */
    public JwtClaims parseToken(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(secretKey)
                // JJWT treats sig() as an allow-list that *augments* the default
                // registry, so add() alone would still accept HS384/HS512 signed
                // with the same key. Clearing first pins verification to HS256.
                .sig().clear().add(Jwts.SIG.HS256).and()
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token);

        // Defence in depth: reject any token whose header does not declare HS256,
        // even if a future parser configuration were to widen the allow-list.
        if (!Jwts.SIG.HS256.getId().equals(jws.getHeader().getAlgorithm())) {
            throw new MalformedJwtException("Access token must be signed with HS256");
        }

        Claims claims = jws.getPayload();

        String subject = claims.getSubject();
        String tokenId = claims.getId();
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);

        if (!StringUtils.hasText(subject)
                || !StringUtils.hasText(tokenId)
                || !StringUtils.hasText(email)
                || !StringUtils.hasText(role)
                || claims.getIssuedAt() == null
                || claims.getExpiration() == null) {
            throw new MalformedJwtException("Access token is missing required claims");
        }

        return new JwtClaims(
                UUID.fromString(subject),
                email,
                role,
                UUID.fromString(tokenId),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        );
    }

    public UUID extractUserId(String token) {
        return parseToken(token).userId();
    }

    public String extractEmail(String token) {
        return parseToken(token).email();
    }

    public String extractRole(String token) {
        return parseToken(token).role();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
