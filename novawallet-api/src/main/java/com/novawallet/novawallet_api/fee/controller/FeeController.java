package com.novawallet.novawallet_api.fee.controller;

import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.exception.BadRequestException;
import com.novawallet.novawallet_api.fee.dto.FeeEstimateRequest;
import com.novawallet.novawallet_api.fee.dto.FeeEstimateResponse;
import com.novawallet.novawallet_api.fee.entity.FeeConfiguration;
import com.novawallet.novawallet_api.fee.enums.FeeType;
import com.novawallet.novawallet_api.fee.service.FeeEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@Tag(name = "Fees", description = "Fee engine and estimation endpoints")
public class FeeController {

    private final FeeEngineService feeEngineService;

    public FeeController(FeeEngineService feeEngineService) {
        this.feeEngineService = feeEngineService;
    }

    @Operation(
            summary = "Estimate transaction fee",
            description = "Calculate the estimated fee for a given transaction type and amount. Requires authentication."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Fee estimate calculated",
                    content = @Content(schema = @Schema(implementation = FeeEstimateResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid transaction type or amount"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized"
            )
    })
    @GetMapping("/fees/estimate")
    public ResponseEntity<ApiResponse<FeeEstimateResponse>> estimateFee(
            @Parameter(description = "Transaction type", example = "TRANSFER") @RequestParam String type,
            @Parameter(description = "Transaction amount in ZMW", example = "100.00") @RequestParam BigDecimal amount
    ) {
        FeeType feeType;
        try {
            feeType = FeeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid transaction type: " + type);
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        FeeEstimateResponse estimate = feeEngineService.estimateFee(feeType, amount);
        return ResponseEntity.ok(ApiResponse.success(estimate, "Fee estimate calculated"));
    }

    // ==================== Admin Endpoints ====================

    @Operation(
            summary = "List all fee configurations",
            description = "Returns all fee configurations. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Fee configurations retrieved"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden — requires ADMIN role"
            )
    })
    @GetMapping("/admin/fees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FeeConfiguration>>> getAllFeeConfigurations() {
        List<FeeConfiguration> configs = feeEngineService.getAllConfigurations();
        return ResponseEntity.ok(ApiResponse.success(configs, "Fee configurations retrieved"));
    }

    @Operation(
            summary = "Get fee configuration by ID",
            description = "Returns a specific fee configuration. Requires ADMIN role."
    )
    @GetMapping("/admin/fees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeeConfiguration>> getFeeConfiguration(
            @PathVariable @Parameter(description = "Fee configuration UUID") UUID id
    ) {
        FeeConfiguration config = feeEngineService.getConfigurationById(id);
        return ResponseEntity.ok(ApiResponse.success(config, "Fee configuration retrieved"));
    }

    @Operation(
            summary = "Update fee configuration",
            description = "Update a fee configuration's parameters. Requires ADMIN role."
    )
    @PutMapping("/admin/fees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeeConfiguration>> updateFeeConfiguration(
            @PathVariable @Parameter(description = "Fee configuration UUID") UUID id,
            @Valid @RequestBody FeeConfiguration updated
    ) {
        FeeConfiguration config = feeEngineService.updateConfiguration(id, updated);
        return ResponseEntity.ok(ApiResponse.success(config, "Fee configuration updated"));
    }
}
