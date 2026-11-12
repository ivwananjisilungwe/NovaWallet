package com.novawallet.novawallet_api.payment.controller;

import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.payment.dto.PayoutRequest;
import com.novawallet.novawallet_api.payment.dto.PayoutResponse;
import com.novawallet.novawallet_api.payment.service.PayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Mobile-money payout endpoint: moves wallet funds to an external
 * mobile-money account through the configured payment gateway.
 *
 * <p>Requires authentication (unlike webhooks, whose shared secret is the
 * auth). Guardrails mirror the withdraw flow — see {@link PayoutService}.</p>
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Payouts", description = "Mobile-money payouts via the payment gateway")
@SecurityRequirement(name = "bearer-jwt")
public class PayoutController {

    private final PayoutService payoutService;

    public PayoutController(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    @PostMapping("/wallets/{walletId}/payouts")
    @Operation(summary = "Pay out to a mobile-money account",
            description = "Debits the wallet and initiates a Lipila disbursement to the given "
                    + "mobile-money account. Requires PIN; subject to velocity and daily-send limits. "
                    + "The disabled-gateway check is performed in the service layer "
                    + "(requirePaymentsEnabled) before the payout flow begins; if payments are "
                    + "disabled the call fails before reaching the provider-status branches below. "
                    + "Supports idempotency via the {@code Idempotency-Key} request header: "
                    + "repeating the same key returns the original payout without re-debiting the wallet. "
                    + "Idempotency policy: keys are scoped to the authenticated user "
                    + "({@code <userId>:<key>}); replaying a key returns the ORIGINAL payout response "
                    + "even if the request payload differs — the key identifies the operation, not the "
                    + "payload, and no payload-hash comparison is performed.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payout initiated (provider returned Successful)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Payout pending provider acceptance (provider returned Pending or uncertain)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient balance, invalid PIN, wallet not active, or provider rejected (Failed)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Wallet does not belong to user or email not verified"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Velocity limit exceeded")
    })
    public ResponseEntity<ApiResponse<PayoutResponse>> payout(
            @PathVariable UUID walletId,
            @Valid @RequestBody PayoutRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        PayoutResponse result = payoutService.payout(walletId, request, userId, idempotencyKey);

        String gatewayStatus = result.gatewayStatus();

        if ("Failed".equalsIgnoreCase(gatewayStatus)) {
            // The full payout flow (initiate → disburse → finalize with compensating credit)
            // has already run inside payoutService.payout(). Throw so the global exception
            // handler produces the standard error body with a `code` field (BAD_REQUEST).
            throw new BadRequestException(result.gatewayMessage());
        }

        if ("Pending".equalsIgnoreCase(gatewayStatus)) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(result, "Payout pending provider acceptance"));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Payout initiated"));
    }
}
