package com.novawallet.novawallet_api.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Lipila payment-gateway settings ({@code app.payments.lipila}).
 *
 * <p>Follows the {@code app.kyc} / {@code app.transaction.velocity}
 * {@code @ConfigurationProperties} precedent: a typed, defaulted config bean
 * bound from {@code application.yml}, with secrets supplied exclusively via
 * environment variables.</p>
 *
 * <p><strong>Safety:</strong> {@code enabled} defaults to {@code false} so the
 * entire payment feature is inert until an operator explicitly flips it on
 * <em>and</em> supplies real credentials. With no {@code LIPILA_API_KEY} /
 * {@code LIPILA_WEBHOOK_SECRET} configured, every outbound call and webhook
 * verification fails closed (503 / 401 respectively).</p>
 */
@Component
@ConfigurationProperties(prefix = "app.payments.lipila")
public class PaymentConfig {

    /**
     * Master switch for the payment integration. When {@code false},
     * disbursement initiation returns 503 SERVICE_UNAVAILABLE and no outbound
     * gateway traffic occurs. Authenticated inbound webhooks are still
     * processed when a webhook secret is configured — see WebhookService.
     */
    private boolean enabled = false;

    /** Lipila REST API base URL. */
    private String baseUrl = "https://api.lipila.dev/api/v1";

    /** Merchant API key sent as the {@code x-api-key} header. From {@code LIPILA_API_KEY}. */
    private String apiKey = "";

    /** Shared secret used to authenticate inbound webhooks. From {@code LIPILA_WEBHOOK_SECRET}. */
    private String webhookSecret = "";

    /**
     * Absolute public URL of our collection webhook
     * ({@code .../api/v1/webhooks/lipila/collections}), sent to Lipila via the
     * optional {@code callbackUrl} header so callbacks reach us without manual
     * dashboard configuration. Blank = header omitted (configure the URL in
     * the Lipila merchant dashboard instead). Bindable from properties/env;
     * intentionally not pre-documented in application.yml because the correct
     * value is deployment-specific.
     */
    private String callbackUrl = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }
}
