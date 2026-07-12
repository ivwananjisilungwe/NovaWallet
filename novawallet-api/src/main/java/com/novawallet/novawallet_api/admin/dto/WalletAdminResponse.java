package com.novawallet.novawallet_api.admin.dto;

import com.novawallet.novawallet_api.wallet.entity.Wallet;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Admin wallet overview")
public record WalletAdminResponse(
        @Schema(description = "Wallet ID", example = "550e8400-e29b-41d4-a716-446655440001")
        String id,

        @Schema(description = "Account number", example = "NW-100001")
        String accountNumber,

        @Schema(description = "Owner user ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String userId,

        @Schema(description = "Current balance", example = "1500.50")
        BigDecimal balance,

        @Schema(description = "Currency", example = "ZMW")
        String currency,

        @Schema(description = "Wallet status", example = "ACTIVE")
        String status,

        @Schema(description = "Freeze reason if frozen", example = "null")
        String freezeReason,

        @Schema(description = "Wallet creation timestamp")
        LocalDateTime createdAt
) {
    public static WalletAdminResponse from(Wallet wallet) {
        return new WalletAdminResponse(
                wallet.getId().toString(),
                wallet.getAccountNumber(),
                wallet.getUserId().toString(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus().name(),
                wallet.getFreezeReason() != null ? wallet.getFreezeReason().name() : null,
                wallet.getCreatedAt()
        );
    }
}
