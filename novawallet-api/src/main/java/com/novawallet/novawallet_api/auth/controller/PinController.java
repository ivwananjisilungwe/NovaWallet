package com.novawallet.novawallet_api.auth.controller;

import com.novawallet.novawallet_api.auth.dto.request.SetPinRequest;
import com.novawallet.novawallet_api.auth.service.AuthService;
import com.novawallet.novawallet_api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/pin")
@Tag(name = "Transaction PIN", description = "Transaction PIN management — set your 4-6 digit transaction PIN")
public class PinController {

    private final AuthService authService;

    public PinController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Set transaction PIN",
            description = "Sets a 4-6 digit transaction PIN for the authenticated user. The PIN is verified automatically during wallet operations (withdrawals, transfers). Requires JWT token."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "PIN set successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "PIN is locked or invalid"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized — missing or invalid JWT"
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> setPin(
            @Valid @RequestBody SetPinRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        authService.setPin(userId, request.pin());
        return ResponseEntity.ok(ApiResponse.success(null, "PIN set successfully"));
    }
}
