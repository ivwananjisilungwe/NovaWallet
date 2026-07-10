package com.novawallet.novawallet_api.user.dto.response;

import com.novawallet.novawallet_api.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User profile information")
public record UserProfileResponse(
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
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId().toString(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.isEmailVerified(),
                user.getPinHash() != null
        );
    }
}
