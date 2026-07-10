package com.novawallet.novawallet_api.admin.dto;

import com.novawallet.novawallet_api.user.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Admin user summary information")
public record UserSummaryResponse(
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

        @Schema(description = "Whether email is verified", example = "true")
        boolean emailVerified,

        @Schema(description = "Whether the user is soft-deleted", example = "false")
        boolean deleted,

        @Schema(description = "Account creation timestamp")
        LocalDateTime createdAt
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId().toString(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.isEmailVerified(),
                user.isDeleted(),
                user.getCreatedAt()
        );
    }
}
