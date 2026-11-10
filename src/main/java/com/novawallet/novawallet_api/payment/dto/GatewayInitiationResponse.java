package com.novawallet.novawallet_api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Result of an initiation call (collection or disbursement) to the gateway.
 *
 * <p>{@code status} mirrors the provider's acceptance state at initiation
 * time — typically {@code Pending} for mobile-money flows, with
 * {@code Successful} / {@code Failed} arriving later via callback.</p>
 */
@Schema(description = "Gateway initiation result")
public record GatewayInitiationResponse(

        @Schema(description = "Merchant reference echoed back by Lipila", example = "NW:3f2c9a1e-...:8b1d-...")
        String referenceId,

        @Schema(description = "Provider status at initiation time", example = "Pending", allowableValues = {"Pending", "Successful", "Failed"})
        String status,

        @Schema(description = "Human-readable provider message", example = "Transaction submitted")
        String message
) {}
