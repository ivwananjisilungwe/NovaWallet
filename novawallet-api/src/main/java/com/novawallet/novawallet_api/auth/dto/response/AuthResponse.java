package com.novawallet.novawallet_api.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response with access and refresh tokens")
public record AuthResponse(
        @Schema(description = "JWT access token for API authorization", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "Refresh token for obtaining a new access token", example = "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...")
        String refreshToken,

        @Schema(description = "Access token expiration time in milliseconds", example = "900000")
        long expiresIn,

        @Schema(description = "Authenticated user's basic information")
        UserInfo user
) {

    @Schema(description = "Basic user information included in auth response")
    public record UserInfo(
            @Schema(description = "User's unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
            String id,

            @Schema(description = "User's first name", example = "John")
            String firstName,

            @Schema(description = "User's last name", example = "Doe")
            String lastName,

            @Schema(description = "User's email address", example = "user@example.com")
            String email,

            @Schema(description = "User's phone number", example = "+260971234567")
            String phone,

            @Schema(description = "User's role", example = "USER")
            String role,

            @Schema(description = "Whether the email has been verified", example = "false")
            boolean emailVerified,

            @Schema(description = "Whether a transaction PIN has been set", example = "false")
            boolean pinSet
    ) {}
}
