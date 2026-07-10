package com.novawallet.novawallet_api.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Admin request to reject KYC with reason")
public record RejectKycRequest(
        @Schema(description = "Rejection reason", example = "ID document is blurry. Please upload a clearer photo.")
        @NotBlank(message = "Rejection reason is required")
        String reason
) {}
