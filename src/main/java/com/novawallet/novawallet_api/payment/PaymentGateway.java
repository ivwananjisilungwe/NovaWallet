package com.novawallet.novawallet_api.payment;

import com.novawallet.novawallet_api.payment.dto.GatewayInitiationResponse;
import com.novawallet.novawallet_api.payment.dto.GatewayPaymentRequest;
import com.novawallet.novawallet_api.payment.dto.GatewayStatusResponse;

/**
 * Provider-agnostic payment-gateway port for mobile-money collection
 * (pulling funds from a customer into a merchant wallet) and disbursement
 * (pushing funds out to a customer's mobile-money account).
 *
 * <p>Implementations must be safe to call while the feature is disabled only
 * in the sense that callers ({@code PayoutService}, {@code WebhookService})
 * gate on {@code PaymentConfig.isEnabled()} / secret presence first — the
 * interface itself performs no gating.</p>
 *
 * <p>All methods are synchronous and may throw runtime exceptions on transport
 * or provider errors; callers translate those into typed API errors.</p>
 */
public interface PaymentGateway {

    /**
     * Initiates a mobile-money collection: prompts the customer at
     * {@code accountNumber} to approve a push of {@code amount} to the merchant.
     *
     * @param request reference/amount/narration/account/currency payload
     * @return provider acceptance state (typically Pending)
     */
    GatewayInitiationResponse initiateCollection(GatewayPaymentRequest request);

    /**
     * Initiates a mobile-money disbursement (payout) from the merchant float
     * to the customer's {@code accountNumber}. Providers commonly require the
     * merchant's egress IP to be whitelisted for this operation.
     *
     * @param request reference/amount/narration/account/currency payload
     * @return provider acceptance state (Pending, Successful, or Failed)
     */
    GatewayInitiationResponse initiateDisbursement(GatewayPaymentRequest request);

    /**
     * Queries the current provider-side status of a previously initiated
     * transaction by its merchant {@code referenceId}.
     */
    GatewayStatusResponse checkStatus(String referenceId);
}
