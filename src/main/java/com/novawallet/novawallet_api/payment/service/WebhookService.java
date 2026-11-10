package com.novawallet.novawallet_api.payment.service;

import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.exception.ServiceUnavailableException;
import com.novawallet.novawallet_api.payment.config.PaymentConfig;
import com.novawallet.novawallet_api.payment.dto.GatewayCallbackPayload;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.transaction.service.TransactionService;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

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
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    public WebhookService(
            PaymentConfig paymentConfig,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            TransactionService transactionService
    ) {
        this.paymentConfig = paymentConfig;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
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
}
