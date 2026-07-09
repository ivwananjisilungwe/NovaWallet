package com.novawallet.novawallet_api.auth.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserInfo user
) {

    public record UserInfo(
            String id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String role,
            boolean emailVerified,
            boolean pinSet
    ) {}
}
