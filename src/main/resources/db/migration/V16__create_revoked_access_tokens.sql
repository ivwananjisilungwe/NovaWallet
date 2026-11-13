-- =============================================================================
-- Phase 1C Batch 1 — durable access-token revocation (DEC-02)
--
-- Access tokens are stateless JWTs with a 15-minute lifetime. Logging out
-- must invalidate the current token immediately, not merely stop refreshing
-- it. Redis and in-memory deny-lists were rejected: the store must survive a
-- restart and be shared by every application instance.
--
-- Only the token identifier (jti) is stored. The bearer token itself is
-- never persisted. jti is the primary key, which makes the per-request
-- lookup an indexed point read and makes duplicate revocation writes
-- idempotent.
--
-- expires_at mirrors the token's exp claim (UTC). Rows are purgeable once
-- expires_at passes; RevokedAccessTokenCleanupJob removes them in bounded
-- batches using idx_revoked_access_tokens_expires_at.
-- =============================================================================

CREATE TABLE IF NOT EXISTS revoked_access_tokens (
    jti        UUID      NOT NULL,
    user_id    UUID      NOT NULL,
    revoked_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_revoked_access_tokens PRIMARY KEY (jti),
    CONSTRAINT fk_revoked_access_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Supports the bounded cleanup job (select ... where expires_at < ? order by expires_at).
CREATE INDEX IF NOT EXISTS idx_revoked_access_tokens_expires_at
    ON revoked_access_tokens (expires_at);

-- Supports operational forensics (which tokens were revoked for a user).
CREATE INDEX IF NOT EXISTS idx_revoked_access_tokens_user_id
    ON revoked_access_tokens (user_id);
