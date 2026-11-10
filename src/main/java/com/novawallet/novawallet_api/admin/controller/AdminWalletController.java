package com.novawallet.novawallet_api.admin.controller;

import com.novawallet.novawallet_api.admin.dto.AuditLogResponse;
import com.novawallet.novawallet_api.admin.dto.FreezeWalletRequest;
import com.novawallet.novawallet_api.admin.dto.TransactionSearchResponse;
import com.novawallet.novawallet_api.admin.dto.WalletAdminResponse;
import com.novawallet.novawallet_api.admin.service.AdminService;
import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.common.dto.PagedResponse;
import com.novawallet.novawallet_api.wallet.dto.WalletResponse;
import com.novawallet.novawallet_api.wallet.entity.WalletStatus;
import com.novawallet.novawallet_api.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller for admin wallet and operations management.
 *
 * <p>All endpoints require {@code ADMIN} role. Supports wallet freeze/unfreeze,
 * listing wallets, searching transactions, and searching audit logs.</p>
 *
 * @see AdminService
 * @see WalletService
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Operations", description = "Administrative wallet and operations endpoints (requires ADMIN role)")
public class AdminWalletController {

    private final AdminService adminService;
    private final WalletService walletService;

    public AdminWalletController(AdminService adminService, WalletService walletService) {
        this.adminService = adminService;
        this.walletService = walletService;
    }

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
            @Valid @RequestBody FreezeWalletRequest request,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        UUID adminId = UUID.fromString(adminDetails.getUsername());
        WalletResponse wallet = walletService.freezeWallet(walletId, request.reason(), adminId);
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
            @PathVariable @Parameter(description = "Wallet UUID", required = true) UUID walletId,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        UUID adminId = UUID.fromString(adminDetails.getUsername());
        WalletResponse wallet = walletService.unfreezeWallet(walletId, adminId);
        return ResponseEntity.ok(ApiResponse.success(wallet, "Wallet unfrozen"));
    }

    @Operation(
            summary = "List all wallets",
            description = "Returns all wallets with optional status filter. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Wallets retrieved"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden"
            )
    })
    @GetMapping("/wallets")
    public ResponseEntity<ApiResponse<PagedResponse<WalletAdminResponse>>> getAllWallets(
            @RequestParam(required = false)
            @Parameter(description = "Filter by status: ACTIVE or FROZEN")
            String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        WalletStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = WalletStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<WalletAdminResponse> result = PagedResponse.from(
                walletService.getAllWallets(pageable, statusFilter));
        return ResponseEntity.ok(ApiResponse.success(result, "Wallets retrieved"));
    }

    @Operation(
            summary = "Search transactions",
            description = "Search transactions by reference, type, status, date range, or wallet. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Transactions retrieved"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden"
            )
    })
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionSearchResponse>>> searchTransactions(
            @RequestParam(required = false)
            @Parameter(description = "Search by transaction reference (partial match)")
            String reference,
            @RequestParam(required = false)
            @Parameter(description = "Filter by type: DEPOSIT, WITHDRAWAL, TRANSFER_DEBIT, TRANSFER_CREDIT, FEE")
            String type,
            @RequestParam(required = false)
            @Parameter(description = "Filter by status: PENDING, SUCCESSFUL, FAILED")
            String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Start date (ISO format: yyyy-MM-dd)")
            LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "End date (ISO format: yyyy-MM-dd)")
            LocalDate dateTo,
            @RequestParam(required = false)
            @Parameter(description = "Filter by wallet UUID (sender or receiver)")
            String walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<TransactionSearchResponse> result = PagedResponse.from(
                adminService.searchTransactions(reference, type, status, dateFrom, dateTo, walletId, pageable));
        return ResponseEntity.ok(ApiResponse.success(result, "Transactions retrieved"));
    }

    @Operation(
            summary = "View audit logs",
            description = "Search audit logs by entity type, action, admin, or date range. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Audit logs retrieved"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden"
            )
    })
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> searchAuditLogs(
            @RequestParam(required = false)
            @Parameter(description = "Filter by entity type (e.g. Wallet, User, KYC)")
            String entityType,
            @RequestParam(required = false)
            @Parameter(description = "Filter by action (e.g. WALLET_FREEZE, USER_TOGGLE_DELETE)")
            String action,
            @RequestParam(required = false)
            @Parameter(description = "Filter by admin UUID who performed the action")
            String performedBy,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Start date (ISO format: yyyy-MM-dd)")
            LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "End date (ISO format: yyyy-MM-dd)")
            LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<AuditLogResponse> result = PagedResponse.from(
                adminService.searchAuditLogs(entityType, action, performedBy, dateFrom, dateTo, pageable));
        return ResponseEntity.ok(ApiResponse.success(result, "Audit logs retrieved"));
    }
}