package com.novawallet.novawallet_api.admin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** DTO: create/update fee config request. */
@Schema(description = "Request to create a new fee configuration")
public record CreateFeeRequest(
        @Schema(description = "Transaction type", example = "TRANSFER")
        @NotNull(message = "Transaction type is required")
        @Size(max = 20, message = "Transaction type must be at most 20 characters")
        String transactionType,

        @Schema(description = "Percentage fee (e.g. 0.005 = 0.5%)", example = "0.005")
        @NotNull(message = "Percentage fee is required")
        @DecimalMin(value = "0.0000", message = "Percentage fee must be non-negative")
        @DecimalMax(value = "1.0000", message = "Percentage fee must be at most 1 (100%)")
        @Digits(integer = 1, fraction = 4, message = "Percentage fee must have at most 4 decimal places")
        BigDecimal percentageFee,

        @Schema(description = "Flat fee amount", example = "2.00")
        @NotNull(message = "Flat fee is required")
        @DecimalMin(value = "0.00", message = "Flat fee must be non-negative")
        @Digits(integer = 10, fraction = 2, message = "Flat fee must have at most 10 integer digits and 2 decimal places")
        BigDecimal flatFee,

        @Schema(description = "Minimum fee", example = "1.00")
        @DecimalMin(value = "0.00", message = "Minimum fee must be non-negative")
        @Digits(integer = 10, fraction = 2, message = "Minimum fee must have at most 10 integer digits and 2 decimal places")
        BigDecimal minFee,

        @Schema(description = "Maximum fee", example = "50.00")
        @DecimalMin(value = "0.00", message = "Maximum fee must be non-negative")
        @Digits(integer = 10, fraction = 2, message = "Maximum fee must have at most 10 integer digits and 2 decimal places")
        BigDecimal maxFee
) {}
