package com.novawallet.novawallet_api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Profile update payload (all fields optional)")
public record UpdateProfileRequest(

        @Schema(description = "Updated first name", example = "John")
        @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
        String firstName,

        @Schema(description = "Updated last name", example = "Smith")
        @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
        String lastName,

        @Schema(description = "Updated phone number with country code", example = "+260987654321")
        @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Phone number must be valid (e.g., +260971234567)")
        String phone
) {}
