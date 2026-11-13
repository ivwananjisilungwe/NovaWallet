package com.novawallet.novawallet_api.payment.service;

import com.novawallet.novawallet_api.audit.service.AuditService;
import com.novawallet.novawallet_api.auth.service.AuthService;
import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.exception.ForbiddenException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.exception.ServiceUnavailableException;
import com.novawallet.novawallet_api.fee.enums.FeeType;
import com.novawallet.novawallet_api.fee.service.FeeEngineService;
import com.novawallet.novawallet_api.idempotency.service.IdempotencyService;
import com.novawallet.novawallet_api.payment.PaymentGateway;
import com.novawallet.novawallet_api.payment.config.PaymentConfig;
import com.novawallet.novawallet_api.payment.dto.GatewayInitiationResponse;
import com.novawallet.novawallet_api.payment.dto.GatewayPaymentRequest;
import com.novawallet.novawallet_api.payment.dto.PayoutRequest;
import com.novawallet.novawallet_api.payment.dto.PayoutResponse;
import com.novawallet.novawallet_api.security.EmailVerificationPolicy;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.transaction.service.TransactionLimitService;
import com.novawallet.novawallet_api.transaction.service.TransactionReferenceGenerator;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.entity.WalletStatus;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates mobile-money payouts using a three-stage, short-transaction
 * pattern:
 *
 * <ol>
 *   <li><b>Stage A — {@link #initiatePayout}</b> (@Transactional, short): authenticate,
 *       validate wallet ownership/status with pessimistic lock, validate amount/fees/limits/PIN,
 *       debit the wallet, create a PENDING payout record with the provider reference,
 *       then COMMIT so the wallet debit is durable.</li>
 *   <li><b>Stage B — {@link #executeDisbursement}</b> (no transaction): call LipilaGateway.initiateDisbursement()
 *       and capture the response.</li>
 *   <li><b>Stage C — {@link #finalizePayout}</b> (@Transactional, short): reload the payout record,
 *       apply the provider response (SUCCESS → finalize; FAILURE → compensating credit +
 *       record audit; TIMEOUT/UNCERTAIN → mark PENDING_RECONCILIATION, preserve the debit).</li>
 * </ol>
 *
 * <p>Wallet debit strategy: <b>Option A — Debit + Compensating Credit</b>. The wallet is debited
 * durably in Stage A. If the provider rejects the payout, Stage C issues a compensating credit
 * and records an audit entry. If the provider times out or returns an uncertain result, the debit
 * is preserved and the transaction is marked PENDING_RECONCILIATION.</p>
 */
@Service
public class PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);

    private static final String STATUS_SUCCESSFUL = "Successful";
    private static final String STATUS_FAILED = "Failed";
    private static final String STATUS_PENDING = "Pending";

    private final PaymentConfig paymentConfig;
    private final PaymentGateway paymentGateway;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionReferenceGenerator referenceGenerator;
    private final TransactionLimitService transactionLimitService;
    private final FeeEngineService feeEngineService;
    private final AuthService authService;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final EmailVerificationPolicy emailVerificationPolicy;

    /** Dedicated template for the short Stage A / Stage C transactions (bypasses self-invocation proxy issues). */
    private final TransactionTemplate transactionTemplate;

    public PayoutService(
            PaymentConfig paymentConfig,
            PaymentGateway paymentGateway,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            TransactionReferenceGenerator referenceGenerator,
            TransactionLimitService transactionLimitService,
            FeeEngineService feeEngineService,
            AuthService authService,
            AuditService auditService,
            IdempotencyService idempotencyService,
            EmailVerificationPolicy emailVerificationPolicy,
            PlatformTransactionManager transactionManager
    ) {
        this.paymentConfig = paymentConfig;
        this.paymentGateway = paymentGateway;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.referenceGenerator = referenceGenerator;
        this.transactionLimitService = transactionLimitService;
        this.feeEngineService = feeEngineService;
        this.authService = authService;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
        this.emailVerificationPolicy = emailVerificationPolicy;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // Short transactions: never let a hung gateway/database call hold a connection indefinitely.
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    /**
     * Result of Stage A: the provider (merchant) reference used for the gateway call,
     * and the NovaWallet payout transaction id used to reload the record in Stage C.
     */
    public record InitiationResult(String providerReferenceId, UUID payoutTransactionId) {}

    // ==================== Public entry point (idempotency-aware) ====================

    /**
     * Top-level entry point invoked by {@link PayoutController}. Runs the three-stage
     * payout flow: initiate (short txn), disburse (no txn), finalize (short txn).
     *
     * @param idempotencyKey client-supplied idempotency key; if a payout with this key
     *                       already exists, the existing record is returned without
     *                       re-debiting the wallet.
     */
    @CacheEvict(value = "walletBalances", key = "#walletId")
    public PayoutResponse payout(UUID walletId, PayoutRequest request, UUID userId, String idempotencyKey) {
        requirePaymentsEnabled();

        // Idempotency: if a payout with this key already exists, return it without
        // re-debiting the wallet.
        //
        // Triple idempotency mechanism (documented precedence):
        //   1. IdempotencyFilter runs first at the servlet layer. It is DB-unique,
        //      keyed <userId>:<rawKey>, and caches the HTTP response. It handles
        //      HTTP-level dedup for the payout endpoint.
        //   2. This description-scan fast path covers sequential direct-service
        //      calls that bypass the filter (e.g. internal service calls), scoped
        //      to userId to avoid false positives across users.
        //   3. The atomic guard lives inside initiatePayout's transaction: after
        //      the wallet pessimistic lock, the idempotency_key lookup sees any
        //      row a winning concurrent request committed (-> replay), and the
        //      unique index on transactions.idempotency_key (V14) rejects the
        //      loser's insert as a DataIntegrityViolationException so the debit
        //      can never be duplicated (S5).
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String idempotentMarker = idempotencyMarker(idempotencyKey);
            List<Transaction> existing = transactionRepository.findByDescriptionContainingByUserId(
                    idempotentMarker, userId);
            Transaction existingPayout = existing.stream()
                    .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                    .findFirst()
                    .orElse(null);
            if (existingPayout != null) {
                log.info("Idempotent payout replay: wallet={}, key={}, txRef={}, status={}",
                        walletId, idempotencyKey, existingPayout.getReference(), existingPayout.getStatus());
                return replayPayout(existingPayout, request);
            }
        }

        // Stage A — durable initiation (short transaction).
        InitiationResult initiation;
        try {
            initiation = initiatePayout(walletId, request, userId, idempotencyKey);
        } catch (IdempotentReplayException e) {
            // Another request created this payout first — replay it without re-debiting.
            log.info("Idempotent payout replay: wallet={}, key={}, txRef={}, status={}",
                    walletId, idempotencyKey, e.existing.getReference(), e.existing.getStatus());
            return replayPayout(e.existing, request);
        } catch (DataIntegrityViolationException e) {
            // S5: a concurrent racer committed this idempotency key first, so the
            // unique index on transactions.idempotency_key (V14) rejected our
            // insert. Our entire Stage A transaction rolled back — no debit was
            // persisted — so we replay the winner's payout instead of failing.
            Transaction existing = findExistingPayoutByKey(userId, idempotencyKey);
            if (existing != null) {
                log.info("Idempotent payout replay after unique-index conflict: wallet={}, key={}, txRef={}, status={}",
                        walletId, idempotencyKey, existing.getReference(), existing.getStatus());
                return replayPayout(existing, request);
            }
            throw e;
        }

        // Stage B — provider call (no transaction). Any provider error (timeout,
        // network error, malformed response) is treated as UNCERTAIN, never FAILED:
        // we synthesize a Pending response so Stage C runs and marks the payout
        // PENDING_RECONCILIATION, preserving the durable Stage A debit.
        GatewayInitiationResponse gatewayResponse;
        try {
            gatewayResponse = executeDisbursement(initiation.providerReferenceId(), request);
        } catch (Exception e) {
            // S7: only Exception is treated as UNCERTAIN. Every Error subtype
            // (VirtualMachineError incl. OutOfMemoryError/StackOverflowError,
            // ThreadDeath, InternalError, UnknownError, ...) propagates to the JVM
            // and is never swallowed. The Stage A debit is already committed, so an
            // escaping Error simply leaves the payout PENDING for the Batch 3
            // reconciliation scheduler — funds are never lost or double-moved.
            // S10: never log raw provider/throwable messages that may contain sensitive data.
            // Never embed provider-derived text in the response sent to the client.
            log.error("Payout disbursement uncertain for wallet={}, txRef={}",
                    walletId, initiation.payoutTransactionId());
            gatewayResponse = uncertainResponse(initiation.providerReferenceId());
        }

        // Stage C — finalize (short transaction).
        Transaction finalized = finalizePayout(initiation.payoutTransactionId(), gatewayResponse, userId);

        return mapToResponse(finalized, request.accountNumber());
    }

    // ==================== Stage A — Durable Payout Initiation (short transaction) ====================

    /**
     * Stage A: run in a short transaction. Authenticate/authorize, validate wallet
     * ownership + status (pessimistic lock), validate amount/fees/limits/PIN, debit
     * the wallet, create a PENDING payout record with the provider reference, then
     * commit so the wallet debit is durable.
     *
     * @return the provider reference (for Stage B) and the payout transaction id (for Stage C).
     */
    public InitiationResult initiatePayout(UUID walletId, PayoutRequest request, UUID userId, String idempotencyKey) {
        return transactionTemplate.execute(status -> {
            requirePaymentsEnabled();

            Wallet wallet = findOwnedActiveWalletWithLock(walletId, userId);
            emailVerificationPolicy.requireVerifiedForMoneyMovement(userId);

            // Storage key for the V14 transactions.idempotency_key column.
            // Scoped per user (matches the IdempotencyFilter convention
            // "<userId>:<rawKey>") so the unique index is enforced per user and
            // two users can safely reuse the same raw key without colliding or
            // replaying each other's payouts.
            String scopedIdempotencyKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                    ? IdempotencyService.scopedKey(userId, idempotencyKey)
                    : null;

            // S5: atomic service-level idempotency for direct-service callers that
            // bypass the servlet IdempotencyFilter. The wallet pessimistic lock
            // above serialises concurrent Stage A transactions on the same wallet;
            // in READ_COMMITTED this lookup then observes the row the winning
            // request committed, and that request is replayed instead of debited
            // a second time. The unique index on transactions.idempotency_key
            // (V14) is the DB-level backstop for any path that bypasses the
            // wallet lock: the loser's insert fails with a
            // DataIntegrityViolationException, rolling the whole Stage A
            // transaction back (debit included) — handled in payout() by
            // replaying the winner's payout.
            if (scopedIdempotencyKey != null) {
                Transaction existing = transactionRepository.findByIdempotencyKey(scopedIdempotencyKey)
                        .orElse(null);
                if (existing != null) {
                    throw new IdempotentReplayException(existing);
                }
            }

            // S5: claim the idempotency key atomically with the wallet debit via the DB
            // unique constraint on idempotency_keys. This closes the race window for direct
            // service calls that bypass the IdempotencyFilter. Only one concurrent request
            // with the same user-scoped key can proceed past this point; the other replays
            // the existing payout (via IdempotentReplayException) or is rejected.
            if (scopedIdempotencyKey != null) {
                boolean acquired;
                if (idempotencyService.getRecord(scopedIdempotencyKey).isPresent()) {
                    // The servlet IdempotencyFilter (HTTP path) or a concurrent direct
                    // call already owns this key — treat as acquired and proceed; the V14
                    // unique index on transactions.idempotency_key is the backstop.
                    acquired = true;
                } else {
                    acquired = idempotencyService.tryAcquire(
                            scopedIdempotencyKey, "POST", "/api/v1/wallets/{walletId}/payouts", userId);
                }
                if (!acquired) {
                    Transaction existing = findExistingPayoutByKey(userId, idempotencyKey);
                    if (existing != null) {
                        throw new IdempotentReplayException(existing);
                    }
                    throw new BadRequestException(
                            "A payout with this idempotency key is already being processed");
                }
            }

            BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_EVEN);

            // Same guardrail stack as withdraw(): velocity before PIN, daily limit after.
            transactionLimitService.enforceVelocityLimits(userId, amount);
            authService.verifyPin(userId, request.pin());
            transactionLimitService.enforceDailySendLimit(userId, walletId, amount);

            BigDecimal fee = feeEngineService.calculateFee(FeeType.WITHDRAWAL, amount);
            BigDecimal totalDebit = amount.add(fee);

            BigDecimal balanceBefore = wallet.getBalance();
            int updated = walletRepository.updateBalance(walletId, totalDebit.negate());
            if (updated == 0) {
                throw new BadRequestException("Insufficient balance (including fee of " + fee + ")");
            }
            wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new IllegalStateException("Wallet disappeared after payout debit"));

            String providerReferenceId = referencePrefix() + walletId + ":" + UUID.randomUUID();

            // Persist PENDING payout record (durable debit) before the gateway call.
            BigDecimal balanceAfterWithoutFee = balanceBefore.subtract(amount);
            Transaction payoutTx = Transaction.builder()
                    .reference(referenceGenerator.generate())
                    .type(TransactionType.WITHDRAWAL)
                    .amount(amount)
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfterWithoutFee)
                    .status(TransactionStatus.PENDING)
                    .description(payoutDescription(request.description(), request.accountNumber(), providerReferenceId, idempotencyKey))
                    .senderWalletId(walletId)
                    // Dedicated payout data (Batch 3 B3.1). The idempotency_key
                    // column stores the USER-SCOPED key so the V14 unique index
                    // is enforced per user.
                    .providerReference(providerReferenceId)
                    .externalStatus(STATUS_PENDING)
                    .payoutAccount(request.accountNumber())
                    .payoutChannel("mobile-money")
                    .attemptCount(0)
                    .idempotencyKey(scopedIdempotencyKey)
                    .build();
            payoutTx = transactionRepository.save(payoutTx);

            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                Transaction feeTx = Transaction.builder()
                        .reference(referenceGenerator.generate())
                        .type(TransactionType.FEE)
                        .amount(fee)
                        .balanceBefore(balanceAfterWithoutFee)
                        .balanceAfter(wallet.getBalance())
                        .status(TransactionStatus.PENDING)
                        .description("Fee for payout " + payoutTx.getReference())
                        .senderWalletId(walletId)
                        .relatedTransactionId(payoutTx.getId())
                        .build();
                transactionRepository.save(feeTx);
            }

            log.info("Payout initiated (Stage A): wallet={}, amount={}, fee={}, txRef={}, gatewayRef={}",
                    walletId, amount, fee, payoutTx.getReference(), providerReferenceId);

            // Record audit event for the durable initiation (Stage A).
            // S10/P3: the idempotency key is deliberately NOT written to the audit
            // trail — audit exposure would enable replay attempts against the payout.
            recordAudit("PAYOUT_INITIATED", userId, walletId,
                    "Payout initiated: txRef=" + payoutTx.getReference() + ", amount=" + amount
                            + ", fee=" + fee + ", providerReference=" + providerReferenceId);

            return new InitiationResult(providerReferenceId, payoutTx.getId());
        });
    }

    // ==================== Stage B — Provider Call (no transaction) ====================

    /**
     * Stage B: call LipilaGateway.initiateDisbursement() outside any transaction and
     * capture the response. Any transport/provider error is translated into a typed
     * API error; the wallet debit from Stage A remains durable.
     */
    public GatewayInitiationResponse executeDisbursement(String providerReferenceId, PayoutRequest request) {
        try {
            return paymentGateway.initiateDisbursement(new GatewayPaymentRequest(
                    providerReferenceId,
                    request.amount(),
                    request.description(),
                    request.accountNumber(),
                    "ZMW"
            ));
        } catch (RuntimeException exception) {
            // Wallet debit from Stage A is durable; do NOT roll it back here.
            // S10: never log raw provider exceptions that may contain sensitive data.
            log.error("Lipila disbursement initiation failed for {}", providerReferenceId);
            throw new BadRequestException(
                    "Payout could not be submitted to the mobile-money provider");
        }
    }

    // ==================== Stage C — Finalize (short transaction) ====================

    /**
     * Stage C: run in a short transaction. Reload the payout record, apply the provider
     * response:
     * <ul>
     *   <li>SUCCESS → mark payout SUCCESSFUL, record audit.</li>
     *   <li>FAILURE → compensating credit (restore funds), mark FAILED, record audit.</li>
     *   <li>TIMEOUT/UNCERTAIN (Pending or anything else) → mark PENDING_RECONCILIATION,
     *       preserve the debit, record audit.</li>
     * </ul>
     */
    /**
     * Stage C (live-flow entry point): finalize with the provider response for
     * an in-flight payout. Replayed/stale responses never resolve a
     * PENDING_RECONCILIATION payout (see {@link #finalizePayout(UUID,
     * GatewayInitiationResponse, UUID, boolean)}).
     */
    public Transaction finalizePayout(UUID payoutTransactionId, GatewayInitiationResponse initiation, UUID userId) {
        return finalizePayout(payoutTransactionId, initiation, userId, false);
    }

    /**
     * Stage C: run in a short transaction. Reload the payout record, apply the provider
     * response:
     * <ul>
     *   <li>SUCCESS → mark payout SUCCESSFUL, record audit.</li>
     *   <li>FAILURE → compensating credit (restore funds), mark FAILED, record audit.</li>
     *   <li>TIMEOUT/UNCERTAIN (Pending or anything else) → mark PENDING_RECONCILIATION,
     *       preserve the debit, record audit.</li>
     * </ul>
     *
     * @param reconciliationResolution true only when the provider status was re-queried
     *        authoritatively by the reconciliation scheduler (Batch 3 B3.2). Only then may
     *        a PENDING_RECONCILIATION payout be resolved with an authoritative SUCCESSFUL
     *        or FAILED outcome. A stale or replayed live-flow response must never resolve
     *        it — an uncertain payout is never failed or refunded without fresh provider
     *        confirmation.
     */
    public Transaction finalizePayout(UUID payoutTransactionId, GatewayInitiationResponse initiation, UUID userId,
                                      boolean reconciliationResolution) {
        if (initiation == null) {
            throw new BadRequestException("Gateway initiation response is required");
        }
        return transactionTemplate.execute(status -> {
            Transaction payoutTx = transactionRepository.findById(payoutTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payout record not found: " + payoutTransactionId));

        if (payoutTx.getType() != TransactionType.WITHDRAWAL) {
            throw new BadRequestException("Transaction " + payoutTransactionId + " is not a payout");
        }

        // Pessimistic lock so concurrent duplicate finalisations are serialised and
        // both observe the same post-commit status. The lock is held before the
        // idempotency guard so two threads finalising the same payout cannot both
        // pass the guard and apply a second compensating credit.
        Wallet wallet = walletRepository.findByIdWithLock(payoutTx.getSenderWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        String providerStatus = initiation.status();
        boolean authoritative = STATUS_SUCCESSFUL.equalsIgnoreCase(providerStatus)
                || STATUS_FAILED.equalsIgnoreCase(providerStatus);

        // Idempotent replay: guard decided from the persisted status inside this
        // TransactionTemplate block, after the wallet pessimistic lock is held —
        // never from a caller parameter or pre-lock snapshot.
        //  - Terminal states (SUCCESSFUL/FAILED) are immutable: no double credit.
        //  - PENDING_RECONCILIATION is immutable to replayed/stale live-flow
        //    responses; only the reconciliation path with a fresh, authoritative
        //    provider outcome may resolve it.
        TransactionStatus currentStatus = payoutTx.getStatus();
        if (currentStatus == TransactionStatus.SUCCESSFUL
                || currentStatus == TransactionStatus.FAILED
                || (currentStatus == TransactionStatus.PENDING_RECONCILIATION
                        && !(reconciliationResolution && authoritative))) {
            return payoutTx;
        }

        // B3.1: read the provider reference from the dedicated column. The legacy
        // [LIPILA:...] description marker is only a fallback for pre-V14 rows;
        // payout information is never extracted from descriptions for new records.
        String providerReferenceId = payoutTx.getProviderReference() != null
                ? payoutTx.getProviderReference()
                : extractProviderReference(payoutTx.getDescription());
        // S10: sanitize provider message before it reaches audit logs / descriptions.
        String providerMessage = sanitizeProviderMessage(initiation.message());

        if (STATUS_SUCCESSFUL.equalsIgnoreCase(providerStatus)) {
            // SUCCESS: finalize the payout.
            payoutTx.setStatus(TransactionStatus.SUCCESSFUL);
            payoutTx.setExternalStatus(STATUS_SUCCESSFUL);
            payoutTx.setDescription(payoutTx.getDescription()
                    + " | finalized SUCCESSFUL by provider");
            transactionRepository.save(payoutTx);

            recordAudit("PAYOUT_SUCCESS", userId, payoutTx.getSenderWalletId(),
                    "Payout " + payoutTx.getReference() + " finalized SUCCESSFUL by provider "
                            + "(referenceId=" + providerReferenceId + ", amount=" + payoutTx.getAmount() + ")");

            log.info("Payout finalized SUCCESSFUL (Stage C): wallet={}, txRef={}, gatewayRef={}",
                    wallet.getId(), payoutTx.getReference(), providerReferenceId);
        } else if (STATUS_FAILED.equalsIgnoreCase(providerStatus)) {
            // FAILURE: compensating credit to restore the debit + fee.
            BigDecimal fee = feeEngineService.calculateFee(FeeType.WITHDRAWAL, payoutTx.getAmount());
            BigDecimal totalDebit = payoutTx.getAmount().add(fee);

            int credited = walletRepository.updateBalance(wallet.getId(), totalDebit);
            if (credited == 0) {
                throw new BadRequestException("Compensating credit failed for wallet " + wallet.getId());
            }

            payoutTx.setStatus(TransactionStatus.FAILED);
            payoutTx.setExternalStatus(STATUS_FAILED);
            payoutTx.setDescription(payoutTx.getDescription()
                    + " | finalized FAILED by provider (compensating credit applied)");
            transactionRepository.save(payoutTx);

            // Compensating credit transaction row.
            Transaction creditTx = Transaction.builder()
                    .reference(referenceGenerator.generate())
                    .type(TransactionType.DEPOSIT)
                    .amount(totalDebit)
                    .balanceBefore(wallet.getBalance())
                    .balanceAfter(wallet.getBalance().add(totalDebit))
                    .status(TransactionStatus.SUCCESSFUL)
                    .description("Compensating credit for failed payout " + payoutTx.getReference())
                    .senderWalletId(wallet.getId())
                    .relatedTransactionId(payoutTx.getId())
                    .build();
            transactionRepository.save(creditTx);

            recordAudit("PAYOUT_FAILED_COMPENSATED", userId, payoutTx.getSenderWalletId(),
                    "Payout " + payoutTx.getReference() + " FAILED by provider; compensating credit of "
                            + totalDebit + " applied to wallet " + wallet.getId()
                            + " (referenceId=" + providerReferenceId + ", message=" + providerMessage + ")");

            log.warn("Payout finalized FAILED with compensating credit (Stage C): wallet={}, txRef={}, gatewayRef={}, message={}",
                    wallet.getId(), payoutTx.getReference(), providerReferenceId, providerMessage);
        } else {
            // TIMEOUT/UNCERTAIN (Pending or anything else): preserve the debit, mark for reconciliation.
            payoutTx.setStatus(TransactionStatus.PENDING_RECONCILIATION);
            payoutTx.setExternalStatus(providerStatus);
            payoutTx.setDescription(payoutTx.getDescription()
                    + " | marked PENDING_RECONCILIATION (provider status=" + providerStatus + ")");
            transactionRepository.save(payoutTx);

            recordAudit("PAYOUT_PENDING_RECONCILIATION", userId, payoutTx.getSenderWalletId(),
                    "Payout " + payoutTx.getReference() + " marked PENDING_RECONCILIATION; provider status="
                            + providerStatus + " (referenceId=" + providerReferenceId + ", message=" + providerMessage + ")");

            log.warn("Payout marked PENDING_RECONCILIATION (Stage C): wallet={}, txRef={}, gatewayRef={}, status={}",
                    wallet.getId(), payoutTx.getReference(), providerReferenceId, providerStatus);
        }

        return payoutTx;
        });
    }

    // ==================== Helpers ====================

    private void requirePaymentsEnabled() {
        if (!paymentConfig.isEnabled()) {
            throw new ServiceUnavailableException(
                    "Mobile-money payouts are not available — payment gateway is disabled");
        }
    }

    private Wallet findOwnedActiveWalletWithLock(UUID walletId, UUID userId) {
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (!wallet.getUserId().equals(userId)) {
            throw new ForbiddenException("Wallet does not belong to the authenticated user");
        }
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BadRequestException("Wallet is not active");
        }
        return wallet;
    }

    /** Merchant-reference prefix matching the webhook routing convention. */
    private static String referencePrefix() {
        return "NW:";
    }

    /** Builds the transaction description embedding the account number, provider reference, and idempotency key. */
    private static String payoutDescription(String description, String accountNumber, String providerReferenceId, String idempotencyKey) {
        StringBuilder sb = new StringBuilder(description);
        sb.append(" [ACCT:").append(accountNumber).append("]");
        sb.append(" [LIPILA:").append(providerReferenceId).append("]");
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            sb.append(" [IDEMPOTENCY:").append(idempotencyKey).append("]");
        }
        return sb.toString();
    }

    /** Extracts the provider reference from the [LIPILA:...] description marker. */
    private static String extractProviderReference(String description) {
        if (description == null) return "";
        int idx = description.indexOf("[LIPILA:");
        if (idx < 0) return "";
        int start = idx + "[LIPILA:".length();
        int end = description.indexOf("]", start);
        if (end < 0) return "";
        return description.substring(start, end);
    }

    /** Extracts the destination account number from the [ACCT:...] description marker. */
    private static String extractAccountNumber(String description) {
        if (description == null) return "";
        int idx = description.indexOf("[ACCT:");
        if (idx < 0) return "";
        int start = idx + "[ACCT:".length();
        int end = description.indexOf("]", start);
        if (end < 0) return "";
        return description.substring(start, end);
    }

    /** Deterministic description marker used for idempotency lookup. */
    private static String idempotencyMarker(String idempotencyKey) {
        return "[IDEMPOTENCY:" + idempotencyKey + "]";
    }

    private PayoutResponse mapToResponse(Transaction payoutTx, String accountNumber) {
        return new PayoutResponse(
                payoutTx.getReference(),
                payoutTx.getSenderWalletId(),
                accountNumber,
                payoutTx.getAmount(),
                gatewayStatus(payoutTx.getStatus()),
                gatewayMessage(payoutTx.getStatus())
        );
    }

    private String gatewayStatus(TransactionStatus status) {
        return switch (status) {
            case SUCCESSFUL -> "Successful";
            case FAILED -> "Failed";
            case PENDING, PENDING_RECONCILIATION -> "Pending";
        };
    }

    /**
     * S10: intentionally returns only hardcoded, provider-agnostic strings.
     * NEVER pass provider-derived text (e.g. initiation.message()) through here —
     * the 400-body regression test in PayoutEnabledIntegrationTest enforces that
     * no provider-internal message ever reaches a customer response.
     */
    private String gatewayMessage(TransactionStatus status) {
        return switch (status) {
            case SUCCESSFUL -> "Payout finalized successfully";
            case FAILED -> "Payout failed; compensating credit applied";
            case PENDING -> "Payout pending provider settlement";
            case PENDING_RECONCILIATION -> "Payout awaiting reconciliation";
        };
    }

    /**
     * S10: redact provider-derived text before it reaches audit logs or any
     * persisted description. Strips digit runs that may be OTPs/PINs/card
     * fragments and trims excessive length.
     */
    static String sanitizeProviderMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String sanitized = message;
        // Redact 4-6 digit runs (potential OTPs/PINs).
        sanitized = sanitized.replaceAll("\\b\\d{4,6}\\b", "***");
        // Redact longer digit runs (MSISDNs / card numbers).
        sanitized = sanitized.replaceAll("\\b\\d{7,}\\b", "***");
        // Bound length to avoid log/audit bloat.
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200) + "...";
        }
        return sanitized;
    }

    /** Returns an uncertain gateway response with a fully sanitized, provider-agnostic message. */
    private static GatewayInitiationResponse uncertainResponse(String providerReferenceId) {
        return new GatewayInitiationResponse(
                providerReferenceId,
                STATUS_PENDING,
                "Provider submission uncertain — payout preserved for reconciliation");
    }

    /**
     * Finds an existing payout by its dedicated idempotency-key column (unique per V14).
     * The column stores the user-scoped key ({@code "<userId>:<rawKey>"}), so lookups
     * must scope too — never replay another user's payout.
     */
    private Transaction findExistingPayoutByKey(UUID userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return transactionRepository.findByIdempotencyKey(
                        IdempotencyService.scopedKey(userId, idempotencyKey))
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                .orElse(null);
    }

    /**
     * Maps an existing payout record to a replay response without re-debiting,
     * re-calling the provider, or re-finalizing.
     */
    private PayoutResponse replayPayout(Transaction existing, PayoutRequest request) {
        // B3.1: dedicated column first; legacy description marker only as fallback.
        String accountNumber = existing.getPayoutAccount() != null
                ? existing.getPayoutAccount()
                : extractAccountNumber(existing.getDescription());
        if (accountNumber.isBlank()) {
            accountNumber = request.accountNumber();
        }
        return mapToResponse(existing, accountNumber);
    }

    /**
     * Thrown when a payout with the same idempotency key already exists; {@link #payout}
     * catches it and replays the existing record without re-debiting (S5).
     */
    private static class IdempotentReplayException extends RuntimeException {
        private final Transaction existing;
        IdempotentReplayException(Transaction existing) {
            this.existing = existing;
        }
    }

    private void recordAudit(String action, UUID userId, UUID walletId, String details) {
        try {
            auditService.recordAction("Wallet", walletId, action, null, details, userId);
        } catch (Exception e) {
            // Audit is best-effort; never fail a payout because audit logging failed.
            log.warn("Audit logging failed for action {} on wallet {}: {}", action, walletId, e.getMessage());
        }
    }
}
