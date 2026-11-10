package com.novawallet.novawallet_api.admin.controller;

import com.novawallet.novawallet_api.admin.dto.UserSummaryResponse;
import com.novawallet.novawallet_api.kyc.dto.ApproveKycRequest;
import com.novawallet.novawallet_api.kyc.dto.KycStatusResponse;
import com.novawallet.novawallet_api.kyc.dto.RejectKycRequest;
import com.novawallet.novawallet_api.kyc.service.AdminKycService;
import com.novawallet.novawallet_api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/**
 * REST controller for admin KYC management operations.
 *
 * <p>All endpoints require {@code ADMIN} role. Supports listing pending KYC submissions,
 * viewing KYC details, downloading KYC documents, and approving/rejecting KYC submissions.</p>
 *
 * @see AdminKycService
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin KYC", description = "Administrative KYC review endpoints (requires ADMIN role)")
public class AdminKycController {

    private final AdminKycService adminKycService;

    public AdminKycController(AdminKycService adminKycService) {
        this.adminKycService = adminKycService;
    }

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
}