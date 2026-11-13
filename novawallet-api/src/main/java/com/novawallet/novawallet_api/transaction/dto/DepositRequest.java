package com.novawallet.novawallet_api.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** DTO: deposit request. */
@Schema(description = "Request to deposit funds into a wallet")
public record DepositRequest(
        @Schema(description = "Amount to deposit", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 integer digits and 2 decimal places")
        @DecimalMax(value = "9999999999999.99", message = "Amount exceeds the supported monetary range")
        BigDecimal amount,

        @Schema(description = "Description of the deposit", example = "Cash deposit at branch")
        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must be at most 255 characters")
        String description
) {}
