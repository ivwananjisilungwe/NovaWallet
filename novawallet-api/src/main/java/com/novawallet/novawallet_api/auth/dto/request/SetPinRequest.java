package com.novawallet.novawallet_api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "PIN set or verification payload")
public record SetPinRequest(

        @Schema(description = "4-6 digit numeric PIN", example = "1234")
        @NotBlank(message = "PIN is required")
        @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 digits")
        @Pattern(regexp = "^\\d{4,6}$", message = "PIN must contain only digits")
        String pin
) {}
