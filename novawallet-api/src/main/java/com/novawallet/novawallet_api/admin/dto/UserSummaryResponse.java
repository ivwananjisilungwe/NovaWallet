package com.novawallet.novawallet_api.admin.dto;

import com.novawallet.novawallet_api.user.entity.User;

import java.time.LocalDateTime;

public record UserSummaryResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        boolean emailVerified,
        boolean deleted,
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
