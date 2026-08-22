package com.novawallet.novawallet_api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Result of a gateway status query for a previously initiated transaction.
 */
@Schema(description = "Gateway status query result")
public record GatewayStatusResponse(

        @Schema(description = "Merchant reference the status was queried for", example = "NW:3f2c9a1e-...:8b1d-...")
        String referenceId,

        @Schema(description = "Current provider status", example = "Successful", allowableValues = {"Pending", "Successful", "Failed"})
        String status,

        @Schema(description = "Human-readable provider message", example = "Collection successful")
        String message
) {}
