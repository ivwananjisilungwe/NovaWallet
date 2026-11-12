package com.novawallet.novawallet_api.transaction.repository;

import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JPA repository for Transaction entities with Specification support. */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByReference(String reference);

    /** Finds a payout (or any transaction) by its dedicated provider reference column. */
    Optional<Transaction> findByProviderReference(String providerReference);

    /**
     * Finds transactions whose description contains the given substring.
     * Used for idempotency lookups (provider reference / idempotency key markers
     * embedded in the description) and for locating payout records by gateway
     * reference. Callers should filter by type/status as needed.
     */
    List<Transaction> findByDescriptionContaining(String substring);

    /**
     * Idempotency fallback for payouts: finds WITHDRAWAL transactions whose description
     * contains the given idempotency marker, scoped to the given user (via sender wallet).
     * <p>
     * Dual idempotency mechanism (documented precedence in {@link
     * com.novawallet.novawallet_api.payment.service.PayoutService#payout}):
     * <ul>
     *   <li>{@link com.novawallet.novawallet_api.common.filter.IdempotencyFilter} runs
     *       first at the servlet layer. It is DB-unique, keyed {@code <userId>:<rawKey>},
     *       and caches the HTTP response. It handles HTTP-level dedup for the payout
     *       endpoint.</li>
     *   <li>This service-level description scan is a fallback for when the filter is
     *       bypassed (e.g. internal service calls). It is scoped to {@code userId} to
     *       avoid false positives across users.</li>
     * </ul>
     * The filter takes precedence; this query only runs when the filter did not already
     * short-circuit the request.
     */
    @Query("SELECT t FROM Transaction t JOIN Wallet w ON t.senderWalletId = w.id "
            + "WHERE t.description LIKE CONCAT('%', :substring, '%') AND w.userId = :userId")
    List<Transaction> findByDescriptionContainingByUserId(
            @Param("substring") String substring, @Param("userId") UUID userId);

    /**
     * Idempotency check for payment-gateway webhooks: the provider reference
     * is embedded in the transaction description as a deterministic marker
     * ({@code [LIPILA:<referenceId>]}), so an exact-match existence query
     * detects already-processed callbacks. Callers acquire the target wallet's
     * pessimistic lock first so concurrent duplicate callbacks serialize and
     * the second one observes the first one's committed row.
     */
    boolean existsByDescription(String description);

    /**
     * Finds the payout transaction carrying the given client idempotency key,
     * backed by the unique index on {@code transactions.idempotency_key} (V14).
     * <p>
     * Used by {@link com.novawallet.novawallet_api.payment.service.PayoutService}
     * for atomic service-level idempotency: inside the Stage A transaction
     * (after the wallet pessimistic lock is acquired) the lookup observes any
     * row the winning concurrent request committed; if the lookup misses, the
     * unique index rejects the loser's insert as a
     * {@code DataIntegrityViolationException} so the debit can never be
     * duplicated. Non-payout rows have a NULL key and never conflict.
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc(
            UUID senderWalletId, UUID receiverWalletId, Pageable pageable);

    long countByReferenceStartingWith(String prefix);

    @Query(value = "SELECT reference FROM transactions WHERE reference LIKE :prefix ORDER BY reference DESC LIMIT 1", nativeQuery = true)
    Optional<String> findMaxReferenceByPrefix(@Param("prefix") String prefix);

    /**
     * Allocates the next global transaction-reference sequence value from the
     * database-backed {@code transaction_reference_seq}. This is safe across
     * JVM instances and concurrent requests; callers must still rely on the
     * database unique constraint on {@code transactions.reference}.
     */
    @Query(value = "SELECT nextval('transaction_reference_seq')", nativeQuery = true)
    Long nextReferenceSequence();

    /**
     * Sums the total outgoing transaction amount (WITHDRAWAL + TRANSFER_DEBIT)
     * for a wallet since the given timestamp. Used for KYC daily-send-limit enforcement.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
            + "WHERE t.senderWalletId = :walletId "
            + "AND t.type IN ('WITHDRAWAL', 'TRANSFER_DEBIT') "
            + "AND t.createdAt >= :since")
    BigDecimal sumDailyOutgoing(@Param("walletId") UUID walletId, @Param("since") LocalDateTime since);

    /**
     * Counts the outgoing transactions (WITHDRAWAL + TRANSFER_DEBIT) a user has made
     * since the given timestamp, across all of the user's wallets. Fee rows are excluded.
     * Used for hourly velocity-limit enforcement (rolling window).
     */
    @Query("SELECT COUNT(t) FROM Transaction t JOIN Wallet w ON t.senderWalletId = w.id "
            + "WHERE w.userId = :userId "
            + "AND t.type IN ('WITHDRAWAL', 'TRANSFER_DEBIT') "
            + "AND t.createdAt >= :since")
    long countOutgoingByUserSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    /**
     * Sums the total outgoing transaction amount (WITHDRAWAL + TRANSFER_DEBIT)
     * a user has sent since the given timestamp, across all of the user's wallets.
     * Fee rows are excluded. Used for hourly velocity-limit enforcement (rolling window).
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t JOIN Wallet w ON t.senderWalletId = w.id "
            + "WHERE w.userId = :userId "
            + "AND t.type IN ('WITHDRAWAL', 'TRANSFER_DEBIT') "
            + "AND t.createdAt >= :since")
    BigDecimal sumOutgoingByUserSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    /**
     * Find PENDING transactions created before the given timestamp.
     * Used by {@link PendingTransactionCleanupJob} to mark stale transactions as FAILED.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.createdAt < :before")
    List<Transaction> findPendingOlderThan(@Param("before") LocalDateTime before);

    /**
     * Backfills missing balance snapshots from the current wallet balance.
     * Payout rows are excluded: rows carrying payout-specific V14 columns
     * (provider_reference / idempotency_key) belong to the three-stage payout
     * lifecycle and must never be rewritten by this maintenance job.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE transactions t
            SET balance_before = COALESCE(
                (SELECT balance FROM wallets WHERE id = t.sender_wallet_id),
                (SELECT balance FROM wallets WHERE id = t.receiver_wallet_id)
            ),
            balance_after = COALESCE(
                (SELECT balance FROM wallets WHERE id = t.sender_wallet_id),
                (SELECT balance FROM wallets WHERE id = t.receiver_wallet_id)
            )
            WHERE t.balance_before IS NULL OR t.balance_after IS NULL
            AND t.provider_reference IS NULL AND t.idempotency_key IS NULL
            """, nativeQuery = true)
    int recalculateMissingBalances();

    // ==================== Payout reconciliation (Batch 3 B3.2) ====================

    /**
     * Bounded batch of payout records due for a provider status check.
     * <p>Eligible: WITHDRAWAL rows with a provider reference in PENDING or
     * PENDING_RECONCILIATION whose next check is due (null or past), that are
     * past the in-flight grace window but inside the 24h reconciliation horizon.
     * The grace window keeps the scheduler from racing a live payout request;
     * the horizon ensures unresolved payouts eventually stop being polled and
     * are escalated to manual investigation instead.</p>
     */
    @Query("SELECT t FROM Transaction t WHERE t.type = 'WITHDRAWAL' "
            + "AND t.providerReference IS NOT NULL "
            + "AND t.status IN ('PENDING', 'PENDING_RECONCILIATION') "
            + "AND (t.nextStatusCheckAt IS NULL OR t.nextStatusCheckAt <= :now) "
            + "AND t.createdAt <= :graceCutoff "
            + "AND t.createdAt >= :horizonCutoff "
            + "ORDER BY t.nextStatusCheckAt ASC")
    List<Transaction> findDuePayoutsForReconciliation(
            @Param("now") LocalDateTime now,
            @Param("graceCutoff") LocalDateTime graceCutoff,
            @Param("horizonCutoff") LocalDateTime horizonCutoff,
            Pageable pageable);

    /**
     * Atomically claims a payout for reconciliation: increments the attempt
     * count and pushes the next check out by a lease. Coordinates concurrent
     * application instances — only the caller whose conditional UPDATE affects
     * one row may poll the provider for this payout.
     * <p>Runs in its own short transaction: the surrounding reconciliation flow
     * must not hold a transaction open across provider HTTP calls.</p>
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    /**
     * Atomically claims a payout for reconciliation using an optimistic-version
     * predicate. The bulk UPDATE increments {@code version} itself so a concurrent
     * claim that committed first bumps the version and causes every later claim
     * for the same payout to affect 0 rows (-> SKIPPED). This prevents two
     * application instances from both polling the provider for the same payout.
     */
    @Query("UPDATE Transaction t SET t.attemptCount = t.attemptCount + 1, "
            + "t.nextStatusCheckAt = :leaseUntil, "
            + "t.version = t.version + 1 "
            + "WHERE t.id = :id AND t.attemptCount < :maxAttempts "
            + "AND (t.nextStatusCheckAt IS NULL OR t.nextStatusCheckAt <= :now) "
            + "AND t.version = :expectedVersion")
    int claimPayoutForReconciliation(
            @Param("id") UUID id,
            @Param("now") LocalDateTime now,
            @Param("leaseUntil") LocalDateTime leaseUntil,
            @Param("maxAttempts") int maxAttempts,
            @Param("expectedVersion") Integer expectedVersion);

    /**
     * Reschedules a payout whose reconciliation attempt produced no authoritative outcome.
     *
     * <p><b>Conditional by design.</b> The predicate only advances a payout whose next
     * check is due ({@code null} or in the past). A payout that has already been
     * <em>parked</em> for manual investigation ({@code next_status_check_at} pushed to
     * the 2999 sentinel) is therefore never un-parked by a later sweep. This is what
     * makes the 24-hour park-and-alert path fire <b>at most once per payout</b>:
     * {@link PendingTransactionCleanupJob} reaps stale PENDING payouts by calling this
     * method, and without the condition a parked payout would be reset to {@code now}
     * on every daily run, re-entering {@code findExpiredPayoutsForAlerting} and
     * re-emitting the admin alert and notification forever.</p>
     *
     * <p><b>Caller:</b> {@link PendingTransactionCleanupJob} only — it routes stale
     * PENDING payouts into the reconciliation cycle by making their next check due
     * immediately. The reconciliation service itself must use
     * {@link #applyPayoutReconciliationBackoff(UUID, int, LocalDateTime, LocalDateTime)}
     * so the backoff ladder is actually applied and a stale worker cannot overwrite
     * a newer lease.</p>
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Transaction t SET t.nextStatusCheckAt = :nextCheckAt "
            + "WHERE t.id = :id "
            + "AND (t.nextStatusCheckAt IS NULL OR t.nextStatusCheckAt <= :now)")
    int reschedulePayoutReconciliation(
            @Param("id") UUID id,
            @Param("now") LocalDateTime now,
            @Param("nextCheckAt") LocalDateTime nextCheckAt);

    /**
     * Applies the reconciliation <b>backoff</b> to a payout this worker still owns.
     *
     * <p><b>Why this is a separate method from {@link #reschedulePayoutReconciliation}.</b>
     * The claim ({@link #claimPayoutForReconciliation}) always writes a lease into
     * {@code next_status_check_at} that is <em>in the future</em>
     * ({@code now + leaseMinutes}). The cleanup-job reschedule above is deliberately
     * conditioned on {@code next_status_check_at <= now}, so after a successful claim
     * that predicate is false and the call affects zero rows. Reusing it here would
     * silently leave the row at the flat five-minute lease and the documented
     * 1m/2m/5m/10m/30m/hourly ladder would never be applied — the retry cadence
     * would be the lease, not the backoff. That is exactly the defect this method
     * closes (Phase 1B finding F1).</p>
     *
     * <p><b>Lease-ownership predicate.</b> The UPDATE only applies when the row is
     * still in the exact state the caller's claim left it in:
     * <ul>
     *   <li>{@code attempt_count = :expectedAttempt} — the claim incremented it from
     *       {@code expectedAttempt - 1} to {@code expectedAttempt}. Any later claim by
     *       another worker increments it again and therefore breaks this predicate,
     *       so a stale worker (one whose lease was already re-claimed) cannot
     *       reschedule.</li>
     *   <li>{@code next_status_check_at <= :leasedUntil} — the value the claim wrote.
     *       A parked payout carries the 2999 sentinel, so a parked payout can never be
     *       un-parked by this call; that keeps the 24-hour park-and-alert path firing
     *       at most once per payout. (An inequality is used deliberately rather than
     *       exact timestamp equality, so the predicate never depends on the database
     *       timestamp precision.)</li>
     * </ul>
     * No optimistic-version predicate is used here because authoritative
     * finalization legitimately bumps {@code version} inside the same worker's pass;
     * {@code attempt_count} is the narrower, correct ownership token because nothing
     * in the finalization path writes to it.</p>
     *
     * <p>A return value of 0 is safe and expected under contention: the payout simply
     * keeps its lease deadline and is re-claimed at the next tick. It never fails the
     * payout and never refunds it.</p>
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Transaction t SET t.nextStatusCheckAt = :nextCheckAt "
            + "WHERE t.id = :id "
            + "AND t.attemptCount = :expectedAttempt "
            + "AND t.nextStatusCheckAt <= :leasedUntil")
    int applyPayoutReconciliationBackoff(
            @Param("id") UUID id,
            @Param("expectedAttempt") int expectedAttempt,
            @Param("leasedUntil") LocalDateTime leasedUntil,
            @Param("nextCheckAt") LocalDateTime nextCheckAt);

    /**
     * Payouts past the 24h horizon that are still due: candidates for the
     * admin alert + park sweep. Never selected for polling again once parked.
     */
    @Query("SELECT t FROM Transaction t WHERE t.type = 'WITHDRAWAL' "
            + "AND t.providerReference IS NOT NULL "
            + "AND t.status IN ('PENDING', 'PENDING_RECONCILIATION') "
            + "AND t.createdAt < :horizonCutoff "
            + "AND (t.nextStatusCheckAt IS NULL OR t.nextStatusCheckAt <= :now) "
            + "ORDER BY t.createdAt ASC")
    List<Transaction> findExpiredPayoutsForAlerting(
            @Param("now") LocalDateTime now,
            @Param("horizonCutoff") LocalDateTime horizonCutoff,
            Pageable pageable);

    /**
     * Parks an unresolved payout for manual investigation by pushing its next
     * check far into the future. The payout's status and funds are left
     * untouched — an unknown payout is never failed and never refunded.
     *
     * <p><b>Conditional by design.</b> The park only applies to a payout whose next
     * check is still due, so the UPDATE affects one row for the first instance that
     * parks it and zero rows for every concurrent or later instance. That makes the
     * {@code parked == 0} check in the reconciliation service meaningful and
     * guarantees a single audit event and a single admin notification per payout,
     * regardless of how many application instances sweep it.</p>
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Transaction t SET t.nextStatusCheckAt = :parkedUntil "
            + "WHERE t.id = :id "
            + "AND (t.nextStatusCheckAt IS NULL OR t.nextStatusCheckAt <= :now)")
    int parkPayoutForManualInvestigation(
            @Param("id") UUID id,
            @Param("now") LocalDateTime now,
            @Param("parkedUntil") LocalDateTime parkedUntil);
}
