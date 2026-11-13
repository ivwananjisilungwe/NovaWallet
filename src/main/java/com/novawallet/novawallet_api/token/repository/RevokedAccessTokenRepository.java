package com.novawallet.novawallet_api.token.repository;

import com.novawallet.novawallet_api.token.entity.RevokedAccessToken;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * JPA repository for revoked access tokens (Phase 1C, DEC-02).
 *
 * <p>Lookups are by primary key ({@code jti}), so the per-request revocation
 * check is an indexed point read. Cleanup reads a bounded page ordered by
 * {@code expires_at} so the purge query can use the expiry index and never
 * scans the whole table.</p>
 */
@Repository
public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, UUID> {

    /**
     * Returns up to {@code pageable.getPageSize()} identifiers of rows whose
     * token has already expired, oldest expiry first.
     */
    @Query("""
            select r.jti
            from RevokedAccessToken r
            where r.expiresAt < :cutoff
            order by r.expiresAt asc, r.jti asc
            """)
    List<UUID> findExpiredJtis(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    /**
     * Deletes the given identifiers in a single statement.
     *
     * @return number of rows actually deleted
     */
    @Modifying
    @Query("delete from RevokedAccessToken r where r.jti in :jtis")
    int deleteByJtiIn(@Param("jtis") Collection<UUID> jtis);
}
