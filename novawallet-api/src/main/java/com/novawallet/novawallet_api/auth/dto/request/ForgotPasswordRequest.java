package com.novawallet.novawallet_api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO: forgot password. */
@Schema(description = "Forgot password request payload")
public record ForgotPasswordRequest(

        @Schema(description = "Registered email address", example = "user@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email
) {}
