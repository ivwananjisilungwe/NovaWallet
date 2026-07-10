package com.novawallet.novawallet_api.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request to withdraw funds from a wallet")
public record WithdrawRequest(
        @Schema(description = "Amount to withdraw", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @Schema(description = "4-6 digit PIN for verification", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "PIN is required")
        @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 characters")
        String pin,

        @Schema(description = "Description of the withdrawal", example = "ATM withdrawal")
        @NotBlank(message = "Description is required")
        String description
) {}
