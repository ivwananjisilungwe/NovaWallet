package com.novawallet.novawallet_api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO: change password request (authenticated). */
@Schema(description = "Change password payload (authenticated)")
public record ChangePasswordRequest(

        @Schema(description = "Current password", example = "OldPass@123")
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @Schema(description = "New password (min 8 characters)", example = "NewPass@456")
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "New password must be at least 8 characters")
        String newPassword
) {}