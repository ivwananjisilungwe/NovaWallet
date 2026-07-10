package com.novawallet.novawallet_api.kyc.dto;

import com.novawallet.novawallet_api.kyc.enums.KycStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "KYC status overview for the current user")
public record KycStatusResponse(
        @Schema(description = "Current KYC status", example = "NOT_SUBMITTED")
        KycStatus kycStatus,

        @Schema(description = "Current KYC tier (0 = unverified)", example = "0")
        int currentTier,

        @Schema(description = "Tier name", example = "Unverified")
        String tierName,

        @Schema(description = "Wallet limit for this tier in ZMW", example = "0")
        String walletLimit,

        @Schema(description = "Daily send limit for this tier in ZMW", example = "0")
        String dailySendLimit,

        @Schema(description = "Submitted documents")
        List<KycDocumentResponse> documents,

        @Schema(description = "When KYC was submitted for review")
        LocalDateTime submittedAt,

        @Schema(description = "When KYC was approved")
        LocalDateTime approvedAt,

        @Schema(description = "Rejection reason (if rejected)")
        String rejectionReason
) {}
