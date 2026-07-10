package com.novawallet.novawallet_api.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Admin request to approve KYC")
public record ApproveKycRequest(
        @Schema(description = "KYC tier to assign", example = "2")
        @NotNull(message = "Tier is required")
        @Min(value = 1, message = "Tier must be at least 1")
        int tier
) {}
