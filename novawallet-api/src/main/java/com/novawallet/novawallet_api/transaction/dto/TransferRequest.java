package com.novawallet.novawallet_api.transaction.dto;

import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** DTO: transfer request (requires PIN). */
@Schema(description = "Request to transfer funds to another wallet")
public record TransferRequest(
        @Schema(description = "Recipient wallet ID", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Recipient wallet ID is required")
        UUID receiverWalletId,

        @Schema(description = "Amount to transfer", example = "25.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 integer digits and 2 decimal places")
        @DecimalMax(value = "9999999999999.99", message = "Amount exceeds the supported monetary range")
        BigDecimal amount,

        @Schema(description = "4-6 digit PIN for verification", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "PIN is required")
        @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 characters")
        @Pattern(regexp = "^\\d{4,6}$", message = "PIN must contain only digits")
        String pin,

        @Schema(description = "Description of the transfer", example = "Payment for services")
        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must be at most 255 characters")
        String description
) {}
