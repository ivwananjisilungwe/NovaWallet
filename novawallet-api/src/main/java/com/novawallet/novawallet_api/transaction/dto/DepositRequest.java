package com.novawallet.novawallet_api.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Request to deposit funds into a wallet")
public record DepositRequest(
        @Schema(description = "Amount to deposit", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @Schema(description = "Description of the deposit", example = "Cash deposit at branch")
        @NotBlank(message = "Description is required")
        String description
) {}
