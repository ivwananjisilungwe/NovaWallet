package com.novawallet.novawallet_api.admin.dto;

import com.novawallet.novawallet_api.wallet.entity.FreezeReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Admin request to freeze a wallet")
public record FreezeWalletRequest(
        @Schema(description = "Reason for freezing the wallet", example = "SUSPICIOUS_ACTIVITY")
        @NotNull(message = "Freeze reason is required")
        FreezeReason reason
) {}
