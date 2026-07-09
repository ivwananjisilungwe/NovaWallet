package com.novawallet.novawallet_api.wallet.dto;

import com.novawallet.novawallet_api.wallet.entity.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        String id,
        String accountNumber,
        BigDecimal balance,
        String currency,
        String status,
        String freezeReason,
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
