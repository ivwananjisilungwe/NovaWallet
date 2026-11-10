package com.novawallet.novawallet_api.payment.controller;

import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.exception.UnauthorizedException;
import com.novawallet.novawallet_api.payment.config.PaymentConfig;
import com.novawallet.novawallet_api.payment.dto.GatewayCallbackPayload;
import com.novawallet.novawallet_api.payment.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Inbound payment-gateway webhooks.
 *
 * <p>This endpoint is {@code permitAll} in SecurityConfig by design: it has no
 * NovaWallet user context, so the shared secret header IS the authentication
 * (compared in constant time). It must never be exposed through proxies that
 * strip arbitrary request headers.</p>
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Payment Webhooks", description = "Inbound Lipila payment callbacks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    /**
     * Header carrying the shared secret on inbound callbacks. Configured
     * operator-side (Lipila dashboard / proxy) to match
     * {@code app.payments.lipila.webhook-secret}.
     */
    static final String WEBHOOK_SECRET_HEADER = "X-Webhook-Secret";

    private final PaymentConfig paymentConfig;
    private final WebhookService webhookService;

    public WebhookController(PaymentConfig paymentConfig, WebhookService webhookService) {
        this.paymentConfig = paymentConfig;
        this.webhookService = webhookService;
    }

    @PostMapping("/lipila/collections")
    @Operation(summary = "Lipila collection callback",
            description = "Receives mobile-money collection status updates from Lipila. "
                    + "Authenticated via the shared secret header.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Callback processed or acknowledged"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Malformed callback payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid webhook secret"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Reference points at an unknown wallet"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Webhooks not configured or collection callbacks contained")
    })
    public ResponseEntity<ApiResponse<String>> lipilaCollectionCallback(
            @RequestHeader(value = WEBHOOK_SECRET_HEADER, required = false) String providedSecret,
            @RequestBody GatewayCallbackPayload payload) {

        verifySecret(providedSecret);

        webhookService.processCollectionCallback(payload);

        return ResponseEntity.ok(ApiResponse.success(null, "Callback processed"));
    }

    // ==================== Helpers ====================

    /**
     * Constant-time secret comparison. A plain {@code String.equals} leaks the
     * secret one character at a time through response-timing analysis;
     * {@link MessageDigest#isEqual} runs in time independent of where the
     * first difference occurs.
     */
    private void verifySecret(String providedSecret) {
        String expected = paymentConfig.getWebhookSecret();
        boolean expectedPresent = expected != null && !expected.isBlank();

        byte[] expectedBytes = expectedPresent
                ? expected.getBytes(StandardCharsets.UTF_8)
                : new byte[0];
        byte[] providedBytes = providedSecret != null
                ? providedSecret.getBytes(StandardCharsets.UTF_8)
                : new byte[0];

        if (!expectedPresent || !MessageDigest.isEqual(expectedBytes, providedBytes)) {
            log.warn("Rejected Lipila webhook: invalid or missing {} header",
                    WEBHOOK_SECRET_HEADER);
            throw new UnauthorizedException("Invalid webhook credentials");
        }
    }
}
