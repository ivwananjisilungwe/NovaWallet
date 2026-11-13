package com.novawallet.novawallet_api.token.service;

import com.novawallet.novawallet_api.token.entity.RevokedAccessToken;
import com.novawallet.novawallet_api.token.repository.RevokedAccessTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Durable access-token revocation store (Phase 1C, DEC-02).
 *
 * <p>Access tokens are stateless, so logging out before their 15-minute
 * expiry only works if the server keeps a record of revoked token
 * identifiers. DEC-02 rules out Redis and in-memory deny-lists: the store is
 * the database, which survives restarts and is shared by every application
 * instance.</p>
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li>Only the {@code jti} is persisted — never the bearer token.</li>
 *   <li>The primary key makes the lookup an indexed point read and makes
 *       duplicate revocation writes idempotent.</li>
 *   <li>{@link #revoke(UUID, UUID, Instant)} runs in its own
 *       {@code REQUIRES_NEW} transaction, so a logout succeeds even when
 *       called from an outer transaction that later rolls back, and a
 *       concurrent duplicate insert cannot poison the caller's transaction.</li>
 *   <li>The caller ({@code JwtAuthFilter}) must treat a failure here as
 *       fatal for the request (fail closed with HTTP 503).</li>
 * </ul>
 */
@Service
public class AccessTokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(AccessTokenRevocationService.class);

    private final RevokedAccessTokenRepository repository;
    private final TransactionTemplate requiresNewTransaction;

    public AccessTokenRevocationService(
            RevokedAccessTokenRepository repository,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Returns {@code true} when the token identifier has been revoked.
     *
     * <p>Any persistence failure propagates to the caller so the filter can
     * reject the request instead of silently accepting a possibly-revoked
     * token.</p>
     */
    @Transactional(readOnly = true)
    public boolean isRevoked(UUID tokenId) {
        return repository.existsById(tokenId);
    }

    /**
     * Records a revocation. Idempotent: revoking the same token twice is a
     * no-op, including concurrent duplicate requests.
     *
     * @param tokenId   token identifier from the verified {@code jti} claim
     * @param userId    token owner
     * @param expiresAt natural token expiry; the row is purgeable afterwards
     * @return {@code true} if this call inserted the row, {@code false} if it was already revoked
     */
    public boolean revoke(UUID tokenId, UUID userId, Instant expiresAt) {
        LocalDateTime revokedAt = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expires = LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC);

        try {
            Boolean inserted = requiresNewTransaction.execute(status -> {
                if (repository.existsById(tokenId)) {
                    return false;
                }

                repository.saveAndFlush(RevokedAccessToken.builder()
                        .jti(tokenId)
                        .userId(userId)
                        .revokedAt(revokedAt)
                        .expiresAt(expires)
                        .build());

                return true;
            });

            if (Boolean.TRUE.equals(inserted)) {
                log.info("Access token revoked: jti={} userId={} expiresAt={}",
                        tokenId, userId, expires);
                return true;
            }

            log.debug("Access token already revoked: jti={}", tokenId);
            return false;
        } catch (DataIntegrityViolationException e) {
            // Two requests revoked the same token concurrently; the primary key
            // rejected the loser. The token is revoked either way.
            log.debug("Access token revocation raced with a concurrent request: jti={}", tokenId);
            return false;
        }
    }

    /**
     * Records a revocation in the caller's current transaction.
     *
     * <p>This variant is used by logout so that access-token and refresh-token
     * revocation either commit together or roll back together. It preserves
     * the primary-key idempotency check; a concurrent duplicate insert is
     * treated as "already revoked".</p>
     */
    @Transactional
    public boolean revokeInCurrentTransaction(UUID tokenId, UUID userId, Instant expiresAt) {
        LocalDateTime revokedAt = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expires = LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC);

        try {
            if (repository.existsById(tokenId)) {
                return false;
            }

            repository.saveAndFlush(RevokedAccessToken.builder()
                    .jti(tokenId)
                    .userId(userId)
                    .revokedAt(revokedAt)
                    .expiresAt(expires)
                    .build());

            return true;
        } catch (DataIntegrityViolationException e) {
            log.debug("Access token revocation raced with a concurrent request: jti={}", tokenId);
            return false;
        }
    }
}
