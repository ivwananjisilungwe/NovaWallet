package com.novawallet.novawallet_api.token.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Maps to the {@code revoked_access_tokens} table (Phase 1C, DEC-02).
 *
 * <p>Durable, database-backed deny-list for individual access tokens. Only
 * the token identifier ({@code jti}) is stored — never the bearer token
 * itself — so a database leak cannot be replayed against the API. Rows are
 * removed once the token would have expired naturally; the row's
 * {@code expires_at} mirrors the token's {@code exp} claim.</p>
 */
@Entity
@Table(name = "revoked_access_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedAccessToken {

    /** Token identifier ({@code jti}); primary key. */
    @Id
    @Column(name = "jti", nullable = false, updatable = false)
    private UUID jti;

    /** Owner of the revoked token. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Instant the token was revoked (UTC). */
    @Column(name = "revoked_at", nullable = false, updatable = false)
    private LocalDateTime revokedAt;

    /** Natural expiry of the token (UTC) — the row is purgeable after this. */
    @Column(name = "expires_at", nullable = false, updatable = false)
    private LocalDateTime expiresAt;
}
