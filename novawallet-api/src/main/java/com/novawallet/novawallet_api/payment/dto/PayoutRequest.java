package com.novawallet.novawallet_api.payment.dto;

import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * API request body for {@code POST /api/v1/wallets/{walletId}/payouts} —
 * moves funds from a NovaWallet wallet to an external mobile-money account
 * via the Lipila disbursement API.
 *
 * <p>Guardrails mirror the withdraw flow: PIN verification, per-user hourly
 * velocity limits, KYC daily-send-limit, and the verified-email gate.</p>
 */
@Schema(description = "Request to pay out wallet funds to a mobile-money account")
public record PayoutRequest(

        @Schema(description = "Amount to pay out", example = "250.00", requiredMode = Schema.RequiredMode.REQUIRED)
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

        @Schema(description = "Destination mobile-money MSISDN", example = "260961234567", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Account number is required")
        @Size(max = 30, message = "Account number must be at most 30 characters")
        @Pattern(regexp = "^\\+?\\d{9,15}$", message = "Account number must be a valid mobile-money number")
        String accountNumber,

        @Schema(description = "Narration for the payout", example = "Salary payout", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Description is required")
        @Size(max = 100, message = "Description must be at most 100 characters")
        String description
) {}
