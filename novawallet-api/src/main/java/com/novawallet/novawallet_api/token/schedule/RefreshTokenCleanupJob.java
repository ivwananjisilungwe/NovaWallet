package com.novawallet.novawallet_api.token.schedule;

import com.novawallet.novawallet_api.token.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled job that deletes expired refresh tokens from the database.
 * <p>
 * Runs daily at 4:30 AM. Prevents the refresh_tokens table from growing
 * indefinitely as users generate new tokens on each login/token rotation.
 * <p>
 * The repository's {@code deleteByExpiresAtBefore} uses a derived query
 * that leverages the existing index on {@code expires_at}.
 * <p>
 * Idempotent — running twice deletes the same (already deleted) records
 * with no effect.
 */
@Component
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupJob(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "0 30 4 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteByExpiresAtBefore(now);
        log.info("Cleaned up expired refresh tokens (cutoff: {})", now);
    }
}
