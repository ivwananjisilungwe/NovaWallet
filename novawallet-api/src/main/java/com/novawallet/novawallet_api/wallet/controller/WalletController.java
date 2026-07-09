package com.novawallet.novawallet_api.wallet.controller;

import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.wallet.dto.WalletResponse;
import com.novawallet.novawallet_api.wallet.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = extractUserId(userDetails);
        WalletResponse wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(wallet, "Wallet retrieved"));
    }

    private UUID extractUserId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}
