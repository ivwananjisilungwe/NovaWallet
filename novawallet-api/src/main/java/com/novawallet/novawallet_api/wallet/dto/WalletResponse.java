package com.novawallet.novawallet_api.wallet.dto;

import com.novawallet.novawallet_api.wallet.entity.Wallet;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Wallet information")
public record WalletResponse(
        @Schema(description = "Wallet's unique identifier", example = "550e8400-e29b-41d4-a716-446655440001")
        String id,

        @Schema(description = "Wallet account number", example = "NW-100001")
        String accountNumber,

        @Schema(description = "Current balance", example = "1500.50")
        BigDecimal balance,

        @Schema(description = "Wallet currency", example = "ZMW")
        String currency,

        @Schema(description = "Wallet status", example = "ACTIVE")
        String status,

        @Schema(description = "Reason if wallet is frozen", example = "null")
        String freezeReason,

        @Schema(description = "Wallet creation timestamp")
        LocalDateTime createdAt
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId().toString(),
                wallet.getAccountNumber(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus().name(),
                wallet.getFreezeReason() != null ? wallet.getFreezeReason().name() : null,
                wallet.getCreatedAt()
        );
    }
}
