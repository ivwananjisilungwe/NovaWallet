package com.novawallet.novawallet_api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @Schema(description = "4-6 digit PIN for verification", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "PIN is required")
        @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 characters")
        String pin,

        @Schema(description = "Destination mobile-money MSISDN", example = "260961234567", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Account number is required")
        String accountNumber,

        @Schema(description = "Narration for the payout", example = "Salary payout", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Description is required")
        String description
) {}
