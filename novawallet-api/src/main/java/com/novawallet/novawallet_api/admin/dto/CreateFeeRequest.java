package com.novawallet.novawallet_api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Request to create a new fee configuration")
public record CreateFeeRequest(
        @Schema(description = "Transaction type", example = "TRANSFER")
        @NotNull(message = "Transaction type is required")
        String transactionType,

        @Schema(description = "Percentage fee (e.g. 0.005 = 0.5%)", example = "0.005")
        @NotNull(message = "Percentage fee is required")
        @DecimalMin(value = "0.0000", message = "Percentage fee must be non-negative")
        BigDecimal percentageFee,

        @Schema(description = "Flat fee amount", example = "2.00")
        @NotNull(message = "Flat fee is required")
        @DecimalMin(value = "0.00", message = "Flat fee must be non-negative")
        BigDecimal flatFee,

        @Schema(description = "Minimum fee", example = "1.00")
        BigDecimal minFee,

        @Schema(description = "Maximum fee", example = "50.00")
        BigDecimal maxFee
) {}
