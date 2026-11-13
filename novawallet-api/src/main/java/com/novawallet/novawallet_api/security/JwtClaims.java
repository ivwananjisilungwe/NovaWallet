package com.novawallet.novawallet_api.security;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable, already-validated representation of an access-token payload.
 *
 * <p>Parsing a bearer token exactly once into this record avoids repeated
 * signature verification and claim extraction inside the filter chain, and
 * gives downstream components (for example the logout endpoint) access to
 * the token identifier ({@code jti}) and expiry without re-parsing the raw
 * credential.</p>
 *
 * @param userId    authenticated user ({@code sub})
 * @param email     user email claim
 * @param role      user role claim (without the {@code ROLE_} prefix)
 * @param tokenId   unique token identifier ({@code jti})
 * @param issuedAt  issuance instant ({@code iat})
 * @param expiresAt expiry instant ({@code exp})
 */
public record JwtClaims(
        UUID userId,
        String email,
        String role,
        UUID tokenId,
        Instant issuedAt,
        Instant expiresAt
) {
}
