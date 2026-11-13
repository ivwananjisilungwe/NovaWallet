package com.novawallet.novawallet_api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Change transaction PIN payload")
public record ChangePinRequest(
        @Schema(description = "Current 4-6 digit PIN", example = "1234")
        @NotBlank(message = "Current PIN is required")
        @Pattern(regexp = "^\\d{4,6}$", message = "Current PIN must contain only digits")
        String currentPin,

        @Schema(description = "New 4-6 digit PIN", example = "5678")
        @NotBlank(message = "New PIN is required")
        @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 digits")
        @Pattern(regexp = "^\\d{4,6}$", message = "New PIN must contain only digits")
        String newPin
) {}
