package com.novawallet.novawallet_api.transaction.controller;

import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.common.dto.PagedResponse;
import com.novawallet.novawallet_api.transaction.dto.DepositRequest;
import com.novawallet.novawallet_api.transaction.dto.TransactionResponse;
import com.novawallet.novawallet_api.transaction.dto.TransferRequest;
import com.novawallet.novawallet_api.transaction.dto.WithdrawRequest;
import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import com.novawallet.novawallet_api.transaction.service.TransactionHistoryService;
import com.novawallet.novawallet_api.transaction.service.TransactionService;
import com.novawallet.novawallet_api.wallet.dto.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@Tag(name = "Transactions", description = "Deposit, withdraw, transfer, history, and balance")
@SecurityRequirement(name = "bearer-jwt")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionHistoryService transactionHistoryService;

    public TransactionController(
            TransactionService transactionService,
            TransactionHistoryService transactionHistoryService
    ) {
        this.transactionService = transactionService;
        this.transactionHistoryService = transactionHistoryService;
    }

    // ==================== Write operations ====================

    @PostMapping("/wallets/{walletId}/deposit")
    @Operation(summary = "Deposit funds", description = "Deposit money into a wallet (increases balance)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Deposit successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Wallet does not belong to user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @PathVariable UUID walletId,
            @Valid @RequestBody DepositRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        TransactionResponse result = transactionService.deposit(walletId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Deposit successful"));
    }

    @PostMapping("/wallets/{walletId}/withdraw")
    @Operation(summary = "Withdraw funds", description = "Withdraw money from a wallet (decreases balance). Requires PIN.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Withdrawal successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient balance, invalid PIN, or wallet not active"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Wallet does not belong to user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @PathVariable UUID walletId,
            @Valid @RequestBody WithdrawRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        TransactionResponse result = transactionService.withdraw(walletId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Withdrawal successful"));
    }

    @PostMapping("/transfers")
    @Operation(summary = "Transfer funds", description = "Transfer money from your wallet to another user's wallet. Requires PIN.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Transfer successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient balance, invalid PIN, self-transfer, or wallet not active"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Receiver wallet not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        TransactionResponse result = transactionService.transfer(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Transfer successful"));
    }

    // ==================== Read operations ====================

    @GetMapping("/wallets/{walletId}/transactions")
    @Operation(summary = "Transaction history",
            description = "Get paginated transaction history for a wallet. Supports optional filtering by type, status, and date range.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction history returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Wallet does not belong to user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getTransactionHistory(
            @PathVariable UUID walletId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        PagedResponse<TransactionResponse> result = transactionHistoryService
                .getTransactionHistory(walletId, userId, type, status, from, to, pageable);

        return ResponseEntity.ok(ApiResponse.success(result, "Transaction history retrieved"));
    }

    @GetMapping("/transactions/{reference}")
    @Operation(summary = "Get transaction by reference",
            description = "Lookup a single transaction by its human-readable reference")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Transaction does not belong to you"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionByReference(
            @PathVariable String reference,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        TransactionResponse result = transactionHistoryService.getTransactionByReference(reference, userId);
        return ResponseEntity.ok(ApiResponse.success(result, "Transaction found"));
    }

    @GetMapping("/wallets/{walletId}/balance")
    @Operation(summary = "Get wallet balance",
            description = "Get current balance and currency for a wallet")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Balance returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Wallet does not belong to user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    public ResponseEntity<ApiResponse<WalletResponse>> getWalletBalance(
            @PathVariable UUID walletId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        WalletResponse result = transactionHistoryService.getWalletBalance(walletId, userId);
        return ResponseEntity.ok(ApiResponse.success(result, "Balance retrieved"));
    }
}
