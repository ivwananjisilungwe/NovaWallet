package com.novawallet.novawallet_api.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Inbound callback payload posted by Lipila to our webhook endpoint
 * ({@code POST /api/v1/webhooks/lipila/collections}).
 *
 * <p>Per the Lipila contract the callback JSON contains:
 * {@code referenceId}, {@code status} (Pending | Successful | Failed),
 * {@code amount}, {@code accountNumber}, {@code paymentType}
 * (MtnMoney | AirtelMoney | ZamtelKwacha), {@code identifier} and
 * {@code message}. Unknown extra fields are ignored so that provider-side
 * additions never break deserialization.</p>
 */
@Schema(description = "Lipila collection/disbursement callback payload")
@JsonIgnoreProperties(ignoreUnknown = true)
public record GatewayCallbackPayload(

        @Schema(description = "Merchant reference echoed back by Lipila", example = "NW:3f2c9a1e-...:8b1d-...", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("referenceId")
        String referenceId,

        @Schema(description = "Final/interim status", example = "Successful", allowableValues = {"Pending", "Successful", "Failed"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("status")
        String status,

        @Schema(description = "Transaction amount", example = "150.00")
        @JsonProperty("amount")
        BigDecimal amount,

        @Schema(description = "Mobile-money MSISDN involved in the transaction", example = "260961234567")
        @JsonProperty("accountNumber")
        String accountNumber,

        @Schema(description = "Payment channel used by the customer", example = "MtnMoney", allowableValues = {"MtnMoney", "AirtelMoney", "ZamtelKwacha"})
        @JsonProperty("paymentType")
        String paymentType,

        @Schema(description = "Provider-side transaction identifier", example = "LP-99182736")
        @JsonProperty("identifier")
        String identifier,

        @Schema(description = "Human-readable provider message", example = "Collection successful")
        @JsonProperty("message")
        String message
) {}
