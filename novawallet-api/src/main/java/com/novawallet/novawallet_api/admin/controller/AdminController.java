package com.novawallet.novawallet_api.admin.controller;

import com.novawallet.novawallet_api.admin.dto.FreezeWalletRequest;
import com.novawallet.novawallet_api.admin.dto.UserSummaryResponse;
import com.novawallet.novawallet_api.admin.service.AdminService;
import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.kyc.dto.ApproveKycRequest;
import com.novawallet.novawallet_api.kyc.dto.KycStatusResponse;
import com.novawallet.novawallet_api.kyc.dto.RejectKycRequest;
import com.novawallet.novawallet_api.kyc.service.AdminKycService;
import com.novawallet.novawallet_api.wallet.dto.WalletResponse;
import com.novawallet.novawallet_api.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrative endpoints (requires ADMIN role)")
public class AdminController {

    private final AdminService adminService;
    private final AdminKycService adminKycService;
    private final WalletService walletService;

    public AdminController(AdminService adminService, AdminKycService adminKycService, WalletService walletService) {
        this.adminService = adminService;
        this.adminKycService = adminKycService;
        this.walletService = walletService;
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
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getAllUsers() {
        List<UserSummaryResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved"));
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
            @PathVariable @io.swagger.v3.oas.annotations.Parameter(description = "User UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000") UUID userId
    ) {
        UserSummaryResponse user = adminService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved"));
    }

    @Operation(
            summary = "Toggle user soft-delete status",
            description = "Soft-deletes or restores a user account. If the user is active they become deleted, if deleted they are restored. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "User delete status toggled"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "User not found"
            )
    })
    @PatchMapping("/users/{userId}/toggle-delete")
    public ResponseEntity<ApiResponse<Void>> toggleUserDeletedStatus(
            @PathVariable @io.swagger.v3.oas.annotations.Parameter(description = "User UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000") UUID userId
    ) {
        adminService.toggleUserDeletedStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User delete status toggled"));
    }

    // ==================== Admin KYC Endpoints ====================

    @Operation(
            summary = "List pending KYC submissions",
            description = "Returns all users whose KYC is pending review. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Pending KYC submissions retrieved"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden"
            )
    })
    @GetMapping("/kyc/pending")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getPendingKyc() {
        List<UserSummaryResponse> pending = adminKycService.getPendingSubmissions().stream()
                .map(UserSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(pending, "Pending KYC submissions retrieved"));
    }

    @Operation(
            summary = "View user KYC details",
            description = "Returns a specific user's KYC status, documents, and tier information. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "KYC details retrieved",
                    content = @Content(schema = @Schema(implementation = KycStatusResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "User not found"
            )
    })
    @GetMapping("/kyc/{userId}")
    public ResponseEntity<ApiResponse<KycStatusResponse>> getUserKycDetail(
            @PathVariable @Parameter(description = "User UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000") UUID userId
    ) {
        KycStatusResponse response = adminKycService.getUserKycDetail(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "KYC details retrieved"));
    }

    @Operation(
            summary = "Download KYC document",
            description = "Download a specific KYC document file. Requires ADMIN role."
    )
    @GetMapping("/kyc/{userId}/documents/{documentId}")
    public ResponseEntity<byte[]> downloadKycDocument(
            @PathVariable @Parameter(description = "User UUID", required = true) UUID userId,
            @PathVariable @Parameter(description = "Document UUID", required = true) UUID documentId
    ) {
        byte[] fileData = adminKycService.getDocumentFile(userId, documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileData);
    }

    @Operation(
            summary = "Approve KYC",
            description = "Approve a user's KYC submission and assign a KYC tier. This automatically creates the user's wallet. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "KYC approved, wallet created"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "KYC not in PENDING status or invalid tier"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "User not found"
            )
    })
    @PostMapping("/kyc/{userId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveKyc(
            @PathVariable @Parameter(description = "User UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000") UUID userId,
            @Valid @RequestBody ApproveKycRequest request,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        UUID adminId = UUID.fromString(adminDetails.getUsername());
        adminKycService.approveKyc(userId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "KYC approved and wallet created"));
    }

    @Operation(
            summary = "Reject KYC",
            description = "Reject a user's KYC submission with a reason. User can re-upload documents and resubmit. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "KYC rejected"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "KYC not in PENDING status or missing reason"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "User not found"
            )
    })
    @PostMapping("/kyc/{userId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectKyc(
            @PathVariable @Parameter(description = "User UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000") UUID userId,
            @Valid @RequestBody RejectKycRequest request,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        UUID adminId = UUID.fromString(adminDetails.getUsername());
        adminKycService.rejectKyc(userId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "KYC rejected"));
    }

    // ==================== Admin Wallet Endpoints ====================

    @Operation(
            summary = "Freeze a wallet",
            description = "Freeze a wallet to prevent all transactions. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Wallet frozen"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Wallet already frozen"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Wallet not found"
            )
    })
    @PostMapping("/wallets/{walletId}/freeze")
    public ResponseEntity<ApiResponse<WalletResponse>> freezeWallet(
            @PathVariable @Parameter(description = "Wallet UUID", required = true) UUID walletId,
            @Valid @RequestBody FreezeWalletRequest request
    ) {
        WalletResponse wallet = walletService.freezeWallet(walletId, request.reason());
        return ResponseEntity.ok(ApiResponse.success(wallet, "Wallet frozen"));
    }

    @Operation(
            summary = "Unfreeze a wallet",
            description = "Restore a frozen wallet to active status. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Wallet unfrozen"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Wallet is not frozen"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Wallet not found"
            )
    })
    @PostMapping("/wallets/{walletId}/unfreeze")
    public ResponseEntity<ApiResponse<WalletResponse>> unfreezeWallet(
            @PathVariable @Parameter(description = "Wallet UUID", required = true) UUID walletId
    ) {
        WalletResponse wallet = walletService.unfreezeWallet(walletId);
        return ResponseEntity.ok(ApiResponse.success(wallet, "Wallet unfrozen"));
    }
}
