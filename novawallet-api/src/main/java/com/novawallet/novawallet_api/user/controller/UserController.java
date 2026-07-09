package com.novawallet.novawallet_api.user.controller;

import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.user.dto.request.UpdateProfileRequest;
import com.novawallet.novawallet_api.user.dto.response.UserProfileResponse;
import com.novawallet.novawallet_api.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = extractUserId(userDetails);
        UserProfileResponse profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved"));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = extractUserId(userDetails);
        UserProfileResponse profile = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile updated"));
    }

    private UUID extractUserId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}
