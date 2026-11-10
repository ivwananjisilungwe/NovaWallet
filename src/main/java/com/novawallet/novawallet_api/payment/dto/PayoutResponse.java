package com.novawallet.novawallet_api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * API response for a payout: the local ledger record plus the provider's
 * acceptance state at initiation time.
 */
@Schema(description = "Payout result")
public record PayoutResponse(

        @Schema(description = "NovaWallet transaction reference", example = "NWTX2026082200000001")
        String transactionReference,

        @Schema(description = "Wallet that was debited")
        UUID walletId,

        @Schema(description = "Destination mobile-money MSISDN", example = "260961234567")
        String accountNumber,

        @Schema(description = "Payout amount", example = "250.00")
        BigDecimal amount,

        @Schema(description = "Provider status at initiation time", example = "Pending", allowableValues = {"Pending", "Successful", "Failed"})
        String gatewayStatus,

        @Schema(description = "Human-readable provider message", example = "Transaction submitted")
        String gatewayMessage
) {}
