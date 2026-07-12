package com.novawallet.novawallet_api.idempotency.service;

import com.novawallet.novawallet_api.idempotency.entity.IdempotencyKey;
import com.novawallet.novawallet_api.idempotency.entity.IdempotencyStatus;
import com.novawallet.novawallet_api.idempotency.repository.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyKeyRepository repository;
    private final int ttlHours;

    public IdempotencyService(
            IdempotencyKeyRepository repository,
            @Value("${app.idempotency.ttl-hours:24}") int ttlHours
    ) {
        this.repository = repository;
        this.ttlHours = ttlHours;
    }

    /**
     * Attempts to atomically acquire an idempotency key for this request.
     * The database unique constraint on {@code idempotency_key} prevents
     * concurrent duplicate inserts from racing requests.
     *
     * @return true if this request acquired the key and should process; false if
     * another request already claimed it
     */
    public boolean tryAcquire(String key, String method, String endpoint, UUID userId) {
        try {
            IdempotencyKey record = IdempotencyKey.builder()
                    .idempotencyKey(key)
                    .method(method)
                    .endpoint(endpoint)
                    .userId(userId)
                    .status(IdempotencyStatus.PROCESSING)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusHours(ttlHours))
                    .build();
            repository.saveAndFlush(record);
            log.debug("Acquired idempotency key: {}", key);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.debug("Idempotency key already exists: {} – another request won the race", key);
            return false;
        }
    }

    /**
     * Marks the idempotency key as COMPLETED with the final response data.
     * Called by the winning request after chain.doFilter completes.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String key, int responseStatus, String responseBody) {
        int updated = repository.complete(key, responseStatus, responseBody, LocalDateTime.now());
        if (updated == 0) {
            log.warn("No idempotency record found to complete for key: {}", key);
        } else {
            log.debug("Completed idempotency key: {} with status {}", key, responseStatus);
        }
    }

    /**
     * Returns the cached response for a completed idempotency key.
     */
    public Optional<IdempotencyKey> getCachedResponse(String key) {
        return repository.findByIdempotencyKey(key)
                .filter(ik -> ik.getStatus() == IdempotencyStatus.COMPLETED);
    }

    /**
     * Returns the current record for a key (any status).
     */
    public Optional<IdempotencyKey> getRecord(String key) {
        return repository.findByIdempotencyKey(key);
    }

    /**
     * Removes a PROCESSING record so the client can retry with the same key.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteProcessing(String key) {
        repository.deleteByIdempotencyKey(key);
    }
}
