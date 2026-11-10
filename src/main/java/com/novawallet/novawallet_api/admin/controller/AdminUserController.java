package com.novawallet.novawallet_api.admin.controller;

import com.novawallet.novawallet_api.admin.dto.UserSummaryResponse;
import com.novawallet.novawallet_api.admin.service.AdminService;
import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for admin user management operations.
 *
 * <p>All endpoints require {@code ADMIN} role. Supports listing users,
 * retrieving user details, and toggling user activation status.</p>
 *
 * @see AdminService
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Users", description = "Administrative user management endpoints (requires ADMIN role)")
public class AdminUserController {

    private final AdminService adminService;

    public AdminUserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(
            summary = "List all users",
            description = "Returns a list of all registered users. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Users retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserSummaryResponse.class)))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden — user does not have ADMIN role"
            )
    })
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<UserSummaryResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<UserSummaryResponse> result = PagedResponse.from(adminService.getAllUsers(pageable));
        return ResponseEntity.ok(ApiResponse.success(result, "Users retrieved"));
    }

    @Operation(
            summary = "Get user by ID",
            description = "Returns a specific user's details by UUID. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "User retrieved",
                    content = @Content(schema = @Schema(implementation = UserSummaryResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "User not found"
            )
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getUserById(
            @PathVariable @Parameter(description = "User UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000") UUID userId
    ) {
        UserSummaryResponse user = adminService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved"));
    }

    @Operation(
            summary = "Deactivate or reactivate a user",
            description = "Soft-deletes (deactivates) or restores (reactivates) a user account. Deactivation also freezes the user's wallet. Reactivation unfreezes it. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "User status toggled"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "User not found"
            )
    })
    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> toggleUserDeletedStatus(
            @PathVariable @Parameter(description = "User UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000") UUID userId,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        UUID adminId = UUID.fromString(adminDetails.getUsername());
        adminService.toggleUserDeletedStatus(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, adminService.isUserDeleted(userId) ? "User deactivated" : "User reactivated"));
    }
}