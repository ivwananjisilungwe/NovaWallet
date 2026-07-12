package com.novawallet.novawallet_api.idempotency.repository;

import com.novawallet.novawallet_api.idempotency.entity.IdempotencyKey;
import com.novawallet.novawallet_api.idempotency.entity.IdempotencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("""
            UPDATE IdempotencyKey ik
            SET ik.responseStatus = :responseStatus,
                ik.responseBody = :responseBody,
                ik.status = 'COMPLETED',
                ik.expiresAt = :completedAt
            WHERE ik.idempotencyKey = :idempotencyKey
            """)
    int complete(
            @Param("idempotencyKey") String idempotencyKey,
            @Param("responseStatus") int responseStatus,
            @Param("responseBody") String responseBody,
            @Param("completedAt") LocalDateTime completedAt
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM IdempotencyKey ik WHERE ik.expiresAt < :threshold")
    int deleteExpired(@Param("threshold") LocalDateTime threshold);

    @Modifying
    void deleteByIdempotencyKey(String idempotencyKey);
}
