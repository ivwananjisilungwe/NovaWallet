package com.novawallet.novawallet_api.wallet.controller;

import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.transaction.dto.StatementSummaryResponse;
import com.novawallet.novawallet_api.transaction.service.TransactionHistoryService;
import com.novawallet.novawallet_api.wallet.dto.WalletResponse;
import com.novawallet.novawallet_api.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets", description = "Wallet management endpoints")
public class WalletController {

    private final WalletService walletService;
    private final TransactionHistoryService transactionHistoryService;

    public WalletController(WalletService walletService, TransactionHistoryService transactionHistoryService) {
        this.walletService = walletService;
        this.transactionHistoryService = transactionHistoryService;
    }

    @Operation(
            summary = "Get my wallet",
            description = "Returns the authenticated user's wallet with balance and status. Requires JWT token."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Wallet retrieved",
                    content = @Content(schema = @Schema(implementation = WalletResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized"
            )
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = extractUserId(userDetails);
        WalletResponse wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(wallet, "Wallet retrieved"));
    }

    @Operation(
            summary = "Get monthly statement",
            description = "Returns a summary of all transactions for the given month. Format: YYYY-MM."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Statement retrieved"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid month format"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized"
            )
    })
    @GetMapping("/{walletId}/statements")
    public ResponseEntity<ApiResponse<StatementSummaryResponse>> getMonthlyStatement(
            @PathVariable UUID walletId,
            @RequestParam @Parameter(description = "Month in YYYY-MM format", example = "2026-08") String month,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = extractUserId(userDetails);
        YearMonth yearMonth = YearMonth.parse(month);
        StatementSummaryResponse statement = transactionHistoryService.getMonthlyStatement(walletId, userId, yearMonth);
        return ResponseEntity.ok(ApiResponse.success(statement, "Statement retrieved"));
    }

    private UUID extractUserId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}
