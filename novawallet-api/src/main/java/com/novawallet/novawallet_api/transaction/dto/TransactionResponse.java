package com.novawallet.novawallet_api.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Transaction details")
public record TransactionResponse(
        @Schema(description = "Transaction ID")
        UUID id,

        @Schema(description = "Human-readable transaction reference", example = "NWTX2026071000000001")
        String reference,

        @Schema(description = "Transaction type", example = "DEPOSIT")
        String type,

        @Schema(description = "Transaction amount", example = "100.00")
        BigDecimal amount,

        @Schema(description = "Balance before the transaction", example = "500.00")
        BigDecimal balanceBefore,

        @Schema(description = "Balance after the transaction", example = "600.00")
        BigDecimal balanceAfter,

        @Schema(description = "Transaction status", example = "SUCCESSFUL")
        String status,

        @Schema(description = "Transaction description")
        String description,

        @Schema(description = "Sender wallet ID")
        UUID senderWalletId,

        @Schema(description = "Receiver wallet ID")
        UUID receiverWalletId,

        @Schema(description = "When the transaction occurred")
        LocalDateTime createdAt
) {}
