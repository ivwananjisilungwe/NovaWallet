package com.novawallet.novawallet_api.transaction.dto;

import java.math.BigDecimal;

/**
 * Monthly statement summary for a wallet.
 */
public record StatementSummaryResponse(
        String month,
        BigDecimal totalDeposits,
        BigDecimal totalWithdrawals,
        BigDecimal totalTransfersIn,
        BigDecimal totalTransfersOut,
        BigDecimal totalFees,
        BigDecimal netChange,
        int transactionCount
) {}
