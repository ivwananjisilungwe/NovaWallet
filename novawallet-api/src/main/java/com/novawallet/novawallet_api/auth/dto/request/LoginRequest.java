package com.novawallet.novawallet_api.auth.dto.request;

import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO: user login. */
@Schema(description = "Login credentials payload")
public record LoginRequest(

        @Schema(description = "User's email address", example = "user@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,

        @Schema(description = "Account password", example = "SecurePass@123")
        @NotBlank(message = "Password is required")
        @Size(max = 128, message = "Password must be at most 128 characters")
        String password
) {}
