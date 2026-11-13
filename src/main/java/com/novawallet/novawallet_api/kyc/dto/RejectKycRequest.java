package com.novawallet.novawallet_api.kyc.dto;

import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** DTO: admin KYC rejection with reason. */
@Schema(description = "Admin request to reject KYC with reason")
public record RejectKycRequest(
        @Schema(description = "Rejection reason", example = "ID document is blurry. Please upload a clearer photo.")
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 500, message = "Rejection reason must be at most 500 characters")
        String reason
) {}
