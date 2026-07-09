package com.novawallet.novawallet_api.auth.controller;

import com.novawallet.novawallet_api.auth.dto.request.*;
import com.novawallet.novawallet_api.auth.dto.response.AuthResponse;
import com.novawallet.novawallet_api.auth.service.AuthService;
import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.exception.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/pin")
    public ResponseEntity<ApiResponse<Void>> setPin(
            @Valid @RequestBody SetPinRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = extractUserId(userDetails);
        authService.setPin(userId, request.pin());
        return ResponseEntity.ok(ApiResponse.success(null, "PIN set successfully"));
    }

    @PostMapping("/pin/verify")
    public ResponseEntity<ApiResponse<Void>> verifyPin(
            @Valid @RequestBody SetPinRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = extractUserId(userDetails);
        authService.verifyPin(userId, request.pin());
        return ResponseEntity.ok(ApiResponse.success(null, "PIN verified successfully"));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam("token") String token
    ) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success(null, "If the email exists, a reset link has been sent")
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        String refreshToken = authHeader.substring(7);
        AuthResponse response = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = extractUserId(userDetails);
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    private UUID extractUserId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}
