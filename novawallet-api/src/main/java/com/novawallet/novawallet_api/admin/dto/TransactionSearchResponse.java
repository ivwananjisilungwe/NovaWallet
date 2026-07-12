package com.novawallet.novawallet_api.admin.dto;

import com.novawallet.novawallet_api.transaction.entity.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Transaction record for admin views")
public record TransactionSearchResponse(
        @Schema(description = "Transaction ID", example = "550e8400-e29b-41d4-a716-446655440002")
        String id,

        @Schema(description = "Transaction reference", example = "TXN-20260712-000001")
        String reference,

        @Schema(description = "Transaction type", example = "TRANSFER")
        String type,

        @Schema(description = "Transaction amount", example = "150.00")
        BigDecimal amount,

        @Schema(description = "Balance before transaction", example = "500.00")
        BigDecimal balanceBefore,

        @Schema(description = "Balance after transaction", example = "350.00")
        BigDecimal balanceAfter,

        @Schema(description = "Transaction status", example = "SUCCESSFUL")
        String status,

        @Schema(description = "Sender wallet ID (if applicable)")
        String senderWalletId,

        @Schema(description = "Receiver wallet ID (if applicable)")
        String receiverWalletId,

        @Schema(description = "Transaction description")
        String description,

        @Schema(description = "Transaction timestamp")
        LocalDateTime createdAt
) {
    public static TransactionSearchResponse from(Transaction tx) {
        return new TransactionSearchResponse(
                tx.getId().toString(),
                tx.getReference(),
                tx.getType().name(),
                tx.getAmount(),
                tx.getBalanceBefore(),
                tx.getBalanceAfter(),
                tx.getStatus().name(),
                tx.getSenderWalletId() != null ? tx.getSenderWalletId().toString() : null,
                tx.getReceiverWalletId() != null ? tx.getReceiverWalletId().toString() : null,
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}
