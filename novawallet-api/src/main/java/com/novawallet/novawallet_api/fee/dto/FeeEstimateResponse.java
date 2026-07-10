package com.novawallet.novawallet_api.fee.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Fee estimate response")
public record FeeEstimateResponse(
        @Schema(description = "Transaction type", example = "TRANSFER")
        String transactionType,

        @Schema(description = "Transaction amount", example = "100.00")
        BigDecimal amount,

        @Schema(description = "Percentage fee applied", example = "1.0000")
        BigDecimal percentageFee,

        @Schema(description = "Flat fee applied", example = "0.00")
        BigDecimal flatFee,

        @Schema(description = "Minimum fee applied", example = "1.00")
        BigDecimal minFee,

        @Schema(description = "Maximum fee applied", example = "50.00")
        BigDecimal maxFee,

        @Schema(description = "Calculated total fee", example = "1.00")
        BigDecimal totalFee
) {}
