package com.novawallet.novawallet_api.auth.controller;

import com.novawallet.novawallet_api.auth.service.AuthService;
import com.novawallet.novawallet_api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/email")
@Tag(name = "Email Verification", description = "Email verification endpoints")
public class VerificationController {

    private final AuthService authService;

    public VerificationController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Verify email address",
            description = "Verifies the user's email address using the token sent during registration."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Email verified successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid or expired verification token"
            )
    })
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam("token") @io.swagger.v3.oas.annotations.Parameter(
                    description = "Email verification token",
                    required = true,
                    example = "abc123-def456-ghi789"
            ) String token
    ) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully"));
    }
}
