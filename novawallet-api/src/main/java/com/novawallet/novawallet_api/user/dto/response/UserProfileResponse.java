package com.novawallet.novawallet_api.user.dto.response;

import com.novawallet.novawallet_api.user.entity.User;

public record UserProfileResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        boolean emailVerified,
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
