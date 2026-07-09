package com.novawallet.novawallet_api.admin.controller;

import com.novawallet.novawallet_api.admin.dto.UserSummaryResponse;
import com.novawallet.novawallet_api.admin.service.AdminService;
import com.novawallet.novawallet_api.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getAllUsers() {
        List<UserSummaryResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved"));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getUserById(
            @PathVariable UUID userId
    ) {
        UserSummaryResponse user = adminService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved"));
    }

    @PatchMapping("/users/{userId}/toggle-delete")
    public ResponseEntity<ApiResponse<Void>> toggleUserDeletedStatus(
            @PathVariable UUID userId
    ) {
        adminService.toggleUserDeletedStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User delete status toggled"));
    }
}
