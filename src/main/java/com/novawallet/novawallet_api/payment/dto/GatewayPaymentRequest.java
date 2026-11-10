package com.novawallet.novawallet_api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Outbound initiation payload sent to the Lipila gateway for both
 * mobile-money collections and disbursements (identical body shape per the
 * Lipila REST contract).
 *
 * <p>Field mapping to the Lipila request body:</p>
 * <ul>
 *   <li>{@code referenceId} — merchant-generated unique id. NovaWallet
 *       convention: {@code NW:&lt;walletId&gt;:&lt;uuid&gt;} so that the
 *       asynchronous collection callback can be routed back to the wallet
 *       that initiated it (see {@code WebhookService}).</li>
 *   <li>{@code amount} — transaction amount.</li>
 *   <li>{@code narration} — free-text description shown on the customer's
 *       statement / used for reconciliation.</li>
 *   <li>{@code accountNumber} — destination (disbursement) or source
 *       (collection) MSISDN, e.g. {@code 260961234567}.</li>
 *   <li>{@code currency} — ISO currency code; only {@code ZMW} is supported.</li>
 * </ul>
 */
@Schema(description = "Lipila mobile-money collection/disbursement initiation payload")
public record GatewayPaymentRequest(

        @Schema(description = "Unique merchant reference (NW:<walletId>:<uuid>)", example = "NW:3f2c9a1e-...:8b1d-...", requiredMode = Schema.RequiredMode.REQUIRED)
        String referenceId,

        @Schema(description = "Transaction amount", example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Schema(description = "Narration shown on statements", example = "NovaWallet payout", requiredMode = Schema.RequiredMode.REQUIRED)
        String narration,

        @Schema(description = "Mobile-money MSISDN", example = "260961234567", requiredMode = Schema.RequiredMode.REQUIRED)
        String accountNumber,

        @Schema(description = "ISO currency code (ZMW only)", example = "ZMW", requiredMode = Schema.RequiredMode.REQUIRED)
        String currency
) {}
