package com.novawallet.novawallet_api.kyc.controller;

import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.kyc.dto.KycDocumentResponse;
import com.novawallet.novawallet_api.kyc.dto.KycStatusResponse;
import com.novawallet.novawallet_api.kyc.enums.KycDocumentType;
import com.novawallet.novawallet_api.kyc.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/v1/kyc")
@Tag(name = "KYC", description = "Know Your Customer — identity verification endpoints")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @Operation(
            summary = "Upload a KYC document",
            description = "Upload a KYC document (National ID, Passport, Selfie, or Proof of Address). Requires authentication. Max 10MB per file, allowed types: jpeg, png, webp, pdf."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Document uploaded",
                    content = @Content(schema = @Schema(implementation = KycDocumentResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid file or KYC already approved/pending"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized — missing or invalid JWT"
            )
    })
    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<KycDocumentResponse>> uploadDocument(
            @Parameter(description = "Document type", required = true, example = "NATIONAL_ID")
            @RequestParam KycDocumentType documentType,

            @Parameter(description = "Document file (jpeg, png, webp, pdf, max 10MB)", required = true)
            @RequestParam("file") MultipartFile file,

            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        KycDocumentResponse response = kycService.uploadDocument(userId, documentType, file);
        return ResponseEntity.ok(ApiResponse.success(response, "Document uploaded successfully"));
    }

    @Operation(
            summary = "Get KYC status",
            description = "Returns the current KYC status, tier, limits, and uploaded documents for the authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "KYC status retrieved",
                    content = @Content(schema = @Schema(implementation = KycStatusResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized"
            )
    })
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<KycStatusResponse>> getKycStatus(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        KycStatusResponse response = kycService.getKycStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "KYC status retrieved"));
    }

    @Operation(
            summary = "Submit KYC documents for review",
            description = "Submit all uploaded documents for admin review. Once submitted, no more documents can be uploaded until the review is complete."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "KYC submitted for review",
                    content = @Content(schema = @Schema(implementation = KycStatusResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "No documents uploaded or already submitted"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized"
            )
    })
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<KycStatusResponse>> submitForReview(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        KycStatusResponse response = kycService.submitForReview(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "KYC documents submitted for review"));
    }
}
