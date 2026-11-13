package com.novawallet.novawallet_api.token.schedule;

import com.novawallet.novawallet_api.token.repository.RevokedAccessTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled purge of access-token revocation rows whose token has expired
 * (Phase 1C, DEC-02).
 *
 * <p>A revocation row is only needed until the token's natural {@code exp};
 * afterwards the JWT is rejected by signature/expiry validation regardless.
 * Deleting those rows keeps the deny-list small and the per-request lookup
 * fast.</p>
 *
 * <p>The purge is deliberately bounded: it reads at most
 * {@value #BATCH_SIZE} identifiers per statement and performs at most
 * {@value #MAX_BATCHES} batches per run. A large backlog is cleared over
 * several nights instead of holding a long transaction or a huge
 * {@code IN} list. Ordering by {@code expires_at} lets the query use the
 * index created in {@code V16}.</p>
 */
@Component
@ConditionalOnProperty(
        name = "app.scheduler.revoked-token-cleanup.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RevokedAccessTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RevokedAccessTokenCleanupJob.class);

    static final int BATCH_SIZE = 1000;
    static final int MAX_BATCHES = 20;

    private final RevokedAccessTokenRepository repository;

    public RevokedAccessTokenCleanupJob(RevokedAccessTokenRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 45 4 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC);
        int deleted = 0;

        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            List<UUID> expired = repository.findExpiredJtis(cutoff, PageRequest.of(0, BATCH_SIZE));
            if (expired.isEmpty()) {
                break;
            }

            deleted += repository.deleteByJtiIn(expired);

            if (expired.size() < BATCH_SIZE) {
                break;
            }
        }

        if (deleted > 0) {
            log.info("Cleaned up {} expired access-token revocation rows (cutoff: {})", deleted, cutoff);
        }
    }
}
