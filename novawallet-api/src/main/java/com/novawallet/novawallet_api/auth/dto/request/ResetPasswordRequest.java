package com.novawallet.novawallet_api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Password reset payload with reset token and new password")
public record ResetPasswordRequest(

        @Schema(description = "Password reset token received via email", example = "abc123-def456-ghi789")
        @NotBlank(message = "Token is required")
        String token,

        @Schema(description = "New password (min 8 characters)", example = "NewSecurePass@456")
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String newPassword
) {}
