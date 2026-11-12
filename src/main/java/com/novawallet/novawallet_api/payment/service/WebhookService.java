package com.novawallet.novawallet_api.payment.service;

import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.exception.UnauthorizedException;
import com.novawallet.novawallet_api.exception.ServiceUnavailableException;
import com.novawallet.novawallet_api.audit.service.AuditService;
import com.novawallet.novawallet_api.payment.PaymentGateway;
import com.novawallet.novawallet_api.payment.config.PaymentConfig;
import com.novawallet.novawallet_api.payment.dto.GatewayCallbackPayload;
import com.novawallet.novawallet_api.payment.dto.GatewayInitiationResponse;
import com.novawallet.novawallet_api.payment.dto.GatewayStatusResponse;
import com.novawallet.novawallet_api.transaction.entity.Transaction;
import com.novawallet.novawallet_api.payment.entity.ProviderWebhookEvent;
import com.novawallet.novawallet_api.payment.entity.ProviderWebhookEventStatus;
import com.novawallet.novawallet_api.payment.repository.ProviderWebhookEventRepository;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.transaction.service.TransactionService;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Processes authenticated Lipila collection callbacks: routes the callback to
 * the originating wallet and credits it through the existing ledger
 * ({@link TransactionService#deposit}), idempotently.
 *
 * <h2>Reference convention</h2>
 * Clients pass {@code referenceId} as {@code NW:&lt;walletId&gt;:&lt;uuid&gt;}
 * when initiating a collection. The callback echoes it back, letting us route
 * the credit without any provider-side lookup table.
 *
 * <h2>Failure semantics (deliberate)</h2>
 * <ul>
 *   <li><strong>Missing/wrong shared secret → 401.</strong> The secret IS the
 *       authentication for this endpoint.</li>
 *   <li><strong>Malformed referenceId → 400.</strong> Not a wallet-routing
 *       problem — the payload itself is invalid.</li>
 *   <li><strong>Well-formed reference but unknown wallet → 404.</strong>
 *       Deliberately NOT swallowed with a 200: a silent 200 would hide
 *       misconfiguration (wrong environment, deleted wallet) and strand real
 *       customer money with no operational signal. The tradeoff is that Lipila
 *       may retry the delivery until we return 2xx — retries are bounded and
 *       each one surfaces the same loud 404 for ops to act on.</li>
 *   <li><strong>Pending / Failed callbacks → 200, no credit.</strong> Valid,
 *       correctly authenticated notifications; only terminal success moves
 *       money. Acknowledging prevents pointless retry storms.</li>
 * </ul>
 *
 * <h2>Idempotency</h2>
 * The credited deposit's description embeds a deterministic marker
 * ({@code [LIPILA:<referenceId>]}). Before crediting we take the target
 * wallet's pessimistic lock and then check existence of that marker — so two
 * concurrent duplicate callbacks serialize on the wallet row and the second
 * observes the first one's committed transaction row and becomes a no-op.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    /** Prefix of our merchant reference convention ({@code NW:<walletId>:<uuid>}). */
    static final String REFERENCE_PREFIX = "NW";

    /** Reference segments expected after splitting on ':' (prefix, walletId, uuid). */
    private static final int REFERENCE_SEGMENTS = 3;

    private static final String STATUS_SUCCESSFUL = "Successful";

    private final PaymentConfig paymentConfig;
    private final PaymentGateway paymentGateway;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final PayoutService payoutService;
    private final ProviderWebhookEventRepository webhookEventRepository;
    private final AuditService auditService;

    /** Dedicated template for the REQUIRES_NEW dedup INSERT — bypasses self-invocation proxy issues. */
    private final TransactionTemplate requiresNewTemplate;

    /**
     * Dedicated template for the short finalization / event-state-update
     * transactions (F2). Runs with PROPAGATION_REQUIRED, so
     * {@link PayoutService#finalizePayout}'s own template participates in it:
     * the authoritative settlement and the event's PROCESSED marking commit
     * atomically, while every provider HTTP call happens strictly outside any
     * transaction.
     */
    private final TransactionTemplate shortTxTemplate;

    private final boolean webhookHmacRequired;
    private final int timestampWindowSeconds;

    public WebhookService(
            PaymentConfig paymentConfig,
            PaymentGateway paymentGateway,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            TransactionService transactionService,
            PayoutService payoutService,
            ProviderWebhookEventRepository webhookEventRepository,
            AuditService auditService,
            PlatformTransactionManager transactionManager,
            @Value("${app.payments.lipila.webhook-hmac-required:true}") boolean webhookHmacRequired,
            @Value("${app.payments.lipila.webhook-timestamp-window-seconds:300}") int timestampWindowSeconds
    ) {
        this.paymentConfig = paymentConfig;
        this.paymentGateway = paymentGateway;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
        this.payoutService = payoutService;
        this.webhookEventRepository = webhookEventRepository;
        this.auditService = auditService;
        this.requiresNewTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.shortTxTemplate = new TransactionTemplate(transactionManager);
        this.shortTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        this.webhookHmacRequired = webhookHmacRequired;
        this.timestampWindowSeconds = timestampWindowSeconds;
    }

    /**
     * Handles one collection callback. Returns normally (HTTP 200) when the
     * callback was processed or is a harmless duplicate/interim status; throws
     * typed exceptions (401 handled in the controller, 400/404 here) when the
     * caller or configuration is wrong.
     */
    @Transactional
    public void processCollectionCallback(GatewayCallbackPayload payload) {
        requireWebhookSecretConfigured();
        validatePayload(payload);

        UUID walletId = extractWalletId(payload.referenceId());

        if (!STATUS_SUCCESSFUL.equalsIgnoreCase(payload.status())) {
            // Pending / Failed: acknowledged, nothing to credit yet (or ever).
            log.info("Lipila callback acknowledged without credit: reference={}, status={}",
                    payload.referenceId(), payload.status());
            return;
        }

        BigDecimal amount = normalizeAmount(payload.amount(), payload.referenceId());

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found for reference: " + payload.referenceId()));
        log.warn("Contained unverified Lipila collection callback: reference={}, wallet={}, claimedAmount={}, providerIdentifier={}",
                payload.referenceId(), wallet.getId(), amount, payload.identifier());
        throw new ServiceUnavailableException(
                "Collection callbacks are temporarily disabled until verified collection initiation is implemented");
    }

    // ==================== Helpers ====================

    /**
     * Inbound webhook processing works whenever the shared secret is
     * configured, independent of {@code enabled}: a callback means money
     * already moved at the provider, so refusing to credit would desync our
     * ledger from reality. The {@code enabled} flag gates outbound initiation
     * (see PayoutService).
     */
    private void requireWebhookSecretConfigured() {
        if (paymentConfig.getWebhookSecret() == null || paymentConfig.getWebhookSecret().isBlank()) {
            throw new ServiceUnavailableException("Payment webhooks are not configured");
        }
    }

    private void validatePayload(GatewayCallbackPayload payload) {
        if (payload == null
                || payload.referenceId() == null || payload.referenceId().isBlank()
                || payload.status() == null || payload.status().isBlank()) {
            throw new BadRequestException("Callback payload must include referenceId and status");
        }
    }

    /**
     * Parses {@code NW:<walletId>:<uuid>} into the wallet UUID.
     * Malformed references are client errors (400); well-formed references
     * pointing at missing wallets fail later with 404.
     */
    static UUID extractWalletId(String referenceId) {
        String[] segments = referenceId.split(":");
        if (segments.length != REFERENCE_SEGMENTS
                || !REFERENCE_PREFIX.equals(segments[0])
                || segments[1].isBlank()) {
            throw new BadRequestException(
                    "referenceId must follow the convention NW:<walletId>:<uuid>, got: " + referenceId);
        }
        try {
            return UUID.fromString(segments[1]);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    "referenceId contains an invalid wallet id: " + referenceId);
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount, String referenceId) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException(
                    "Successful callback must carry a positive amount, got: " + amount
                            + " (reference " + referenceId + ")");
        }
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Deterministic description embedding the idempotency marker. */
    static String depositDescription(String referenceId) {
        return "Lipila mobile-money collection [LIPILA:" + referenceId + "]";
    }

    // ==================== Disbursement webhooks (Batch 3 B3.3) ====================

    /** Outcome of a disbursement webhook delivery. */
    public enum DisbursementWebhookResult { PROCESSED, DUPLICATE, QUARANTINED, DEFERRED }

    /**
     * Processes one disbursement callback.
     *
     * <p><b>Transaction boundaries (F2 correction).</b> This method is NOT
     * {@code @Transactional}. The pipeline deliberately separates durable state
     * changes from provider I/O so no database connection is held across the
     * HTTP request:
     * <ol>
     *   <li>Cheap validation (secret, payload, timestamp window, HMAC) — no
     *       transaction.</li>
     *   <li>Durable event claim — {@link #persistEvent} inserts in a REQUIRES_NEW
     *       transaction that commits immediately. Duplicate deliveries fail the
     *       {@code (provider, event_id)} unique constraint here and return
     *       {@link DisbursementWebhookResult#DUPLICATE}.</li>
     *   <li>Reference / amount validation and interim-status acknowledgement —
     *       short transactions for the event-state writes only.</li>
     *   <li><b>Authoritative provider re-query — outside any transaction.</b></li>
     *   <li>Finalization — one short transaction in which
     *       {@link PayoutService#finalizePayout} (replay-guarded; pessimistic
     *       wallet lock inside) settles the payout and the event is marked
     *       {@code PROCESSED}: both commit atomically.</li>
     * </ol>
     * A callback is never trusted alone: the provider is always re-queried
     * before any financial effect, and duplicate deliveries never produce
     * duplicate effects.</p>
     */
    public DisbursementWebhookResult processDisbursementCallback(
            GatewayCallbackPayload payload,
            String eventId,
            String timestamp,
            String signature,
            String rawBody
    ) {
        requireWebhookSecretConfigured();
        validatePayload(payload);

        // Replay window: reject stale or future-dated callbacks before anything else.
        Instant receivedAt;
        try {
            receivedAt = parseTimestamp(timestamp);
        } catch (Exception e) {
            throw new BadRequestException("Webhook timestamp outside acceptable window");
        }
        if (receivedAt == null) {
            throw new BadRequestException("Webhook timestamp outside acceptable window");
        }
        Instant now = Instant.now();
        if (receivedAt.isBefore(now.minusSeconds(timestampWindowSeconds))
                || receivedAt.isAfter(now.plusSeconds(120))) {
            throw new BadRequestException("Webhook timestamp outside acceptable window");
        }

        // HMAC over the raw body (when the provider signs and the operator
        // requires it). Graceful degradation: if not required, a missing/invalid
        // signature is logged but does not reject — the shared-secret + timestamp
        // window remain enforced.
        // Lipila (Standard Webhooks) scheme: HMAC-SHA256 over
        //   {webhook-id}.{webhook-timestamp}.{raw_body}, secret base64-decoded,
        //   output base64, prefixed "v1,". Multiple space-delimited signatures
        //   are accepted (key rotation).
        boolean signatureOk = verifySignature(eventId, timestamp, rawBody, signature);
        if (webhookHmacRequired && !signatureOk) {
            throw new UnauthorizedException("Invalid webhook signature");
        }

        // Durable dedup: persist the event first. A duplicate delivery fails the
        // (provider, event_id) unique constraint and becomes a no-op.
        ProviderWebhookEvent event;
        try {
            event = persistEvent("lipila", eventId, payload.referenceId(),
                    payload.status(), rawBody, signatureOk);
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate disbursement webhook ignored: eventId={}", eventId);
            return DisbursementWebhookResult.DUPLICATE;
        }

        // Match the payout record by its dedicated provider reference column.
        Optional<Transaction> payoutOpt = transactionRepository.findByProviderReference(payload.referenceId());
        if (payoutOpt.isEmpty()) {
            markEventQuarantined(event, "unknown reference", payload.referenceId(), null);
            return DisbursementWebhookResult.QUARANTINED;
        }
        Transaction payoutTx = payoutOpt.get();

        // Amount is required for any terminal (non-Pending) callback so it can be
        // verified against the authoritative payout record. A missing amount on a
        // terminal callback is quarantined (zero financial effect) rather than
        // trusted, and a Pending interim callback is deferred without amount
        // verification (amount is optional for interim status notifications).
        if (payload.amount() == null && !"Pending".equalsIgnoreCase(payload.status())) {
            markEventQuarantined(event, "missing amount on terminal callback",
                    payload.referenceId(), payoutTx.getId());
            return DisbursementWebhookResult.QUARANTINED;
        }

        // Amount match against the persisted payout (currency is platform-wide ZMW).
        if (payload.amount() != null
                && payload.amount().setScale(2, RoundingMode.HALF_EVEN)
                        .compareTo(payoutTx.getAmount()) != 0) {
            markEventQuarantined(event, "amount mismatch: claimed=" + payload.amount()
                    + ", persisted=" + payoutTx.getAmount(), payload.referenceId(), payoutTx.getId());
            return DisbursementWebhookResult.QUARANTINED;
        }

        // Interim status: acknowledge and let the scheduler poll it later.
        if ("Pending".equalsIgnoreCase(payload.status())) {
            markEventStatus(event, ProviderWebhookEventStatus.DEFERRED);
            return DisbursementWebhookResult.DEFERRED;
        }

        // Authoritative re-query — OUTSIDE any database transaction: the provider
        // is the source of truth and no DB connection is held during the call.
        UUID userId = walletRepository.findById(payoutTx.getSenderWalletId())
                .map(Wallet::getUserId)
                .orElse(null);
        GatewayStatusResponse providerStatus;
        try {
            providerStatus = paymentGateway.checkStatus(payload.referenceId());
        } catch (Exception e) {
            log.warn("Disbursement webhook deferred — provider re-query failed for {}", payload.referenceId());
            markEventStatus(event, ProviderWebhookEventStatus.DEFERRED);
            return DisbursementWebhookResult.DEFERRED;
        }

        // Finalization: settle the payout (replay-guarded inside finalizePayout)
        // and mark the event PROCESSED in one short, atomic transaction.
        try {
            shortTxTemplate.execute(status -> {
                payoutService.finalizePayout(payoutTx.getId(),
                        new GatewayInitiationResponse(providerStatus.referenceId(),
                                providerStatus.status(), providerStatus.message()),
                        userId, true);
                event.setProcessingStatus(ProviderWebhookEventStatus.PROCESSED);
                webhookEventRepository.save(event);
                return null;
            });
        } catch (Exception e) {
            log.warn("Disbursement webhook deferred — finalization failed for {}", payload.referenceId());
            markEventStatus(event, ProviderWebhookEventStatus.DEFERRED);
            return DisbursementWebhookResult.DEFERRED;
        }

        return DisbursementWebhookResult.PROCESSED;
    }

    /** Marks an event QUARANTINED in its own short transaction, with audit. */
    private void markEventQuarantined(ProviderWebhookEvent event, String reason,
                                      String referenceId, UUID payoutTxId) {
        log.warn("Disbursement webhook quarantined — {}: reference={}", reason, referenceId);
        markEventStatus(event, ProviderWebhookEventStatus.QUARANTINED);
        if (payoutTxId != null) {
            auditService.recordAction("Transaction", payoutTxId, "PAYOUT_WEBHOOK_QUARANTINED",
                    null, reason, null);
        } else {
            auditService.recordAction("WebhookEvent", event.getId(), "PAYOUT_WEBHOOK_QUARANTINED",
                    null, reason, null);
        }
    }

    /** Persists an event status transition in its own short transaction. */
    private void markEventStatus(ProviderWebhookEvent event, ProviderWebhookEventStatus status) {
        shortTxTemplate.execute(txStatus -> {
            event.setProcessingStatus(status);
            webhookEventRepository.save(event);
            return null;
        });
    }

    /**
     * Persists the event in a REQUIRES_NEW transaction (via the dedicated
     * template, bypassing self-invocation proxy issues). A duplicate delivery
     * fails the (provider, event_id) unique constraint inside that inner
     * transaction, which rolls back independently and propagates as a
     * DataIntegrityViolationException — the outer transaction is unaffected.
     */
    public ProviderWebhookEvent persistEvent(
            String provider,
            String eventId,
            String referenceId,
            String eventType,
            String payload,
            boolean signatureOk
    ) {
        return requiresNewTemplate.execute(status -> {
            ProviderWebhookEvent event = ProviderWebhookEvent.builder()
                    .provider(provider)
                    .eventId(eventId)
                    .referenceId(referenceId)
                    .eventType(eventType)
                    .payload(payload)
                    .signatureOk(signatureOk)
                    .build();
            return webhookEventRepository.saveAndFlush(event);
        });
    }

    private Instant parseTimestamp(String timestamp) {
        try {
            return Instant.ofEpochSecond(Long.parseLong(timestamp));
        } catch (NumberFormatException ignored) {
            return Instant.parse(timestamp);
        }
    }

    /**
     * Verifies a Lipila (Standard Webhooks) signature.
     *
     * <p>Signed content: {@code <webhookId>.<timestamp>.<rawBody>}.</p>
     * <p>Secret is base64-decoded (Lipila issues base64 32-byte secrets); if
     * decoding fails (e.g. a legacy plain-text secret), the raw UTF-8 bytes
     * are used as a fallback so existing deployments are not hard-broken.</p>
     * <p>Output is base64, prefixed {@code v1,}. The {@code webhook-signature}
     * header may contain multiple space-delimited signatures (key rotation);
     * each is tried until one matches (constant-time).</p>
     */
    private boolean verifySignature(String webhookId, String timestamp, String rawBody, String providedSignature) {
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }
        String secret = paymentConfig.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            return false;
        }
        try {
            byte[] key = decodeSecret(secret);
            String signedPayload = (webhookId == null ? "" : webhookId) + "."
                    + (timestamp == null ? "" : timestamp) + "."
                    + (rawBody == null ? "" : rawBody);
            String expected = "v1," + hmacBase64(key, signedPayload);

            for (String candidate : providedSignature.split("\\s+")) {
                String sig = candidate.trim();
                if (sig.isEmpty()) continue;
                if (MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        sig.getBytes(StandardCharsets.UTF_8))) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("Webhook HMAC computation failed");
            return false;
        }
    }

    /** Base64-decode the Lipila secret; fall back to raw UTF-8 bytes on failure. */
    private static byte[] decodeSecret(String secret) {
        try {
            return java.util.Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException e) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static String hmacBase64(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }
}
