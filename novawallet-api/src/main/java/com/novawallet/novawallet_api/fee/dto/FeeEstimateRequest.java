package com.novawallet.novawallet_api.fee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Fee estimate request")
public record FeeEstimateRequest(
        @Schema(description = "Transaction type", example = "TRANSFER")
        @NotNull String type,

        @Schema(description = "Transaction amount in ZMW", example = "100.00")
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {}
