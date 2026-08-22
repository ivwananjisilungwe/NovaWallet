package com.novawallet.novawallet_api.payment;

import com.novawallet.novawallet_api.payment.config.PaymentConfig;
import com.novawallet.novawallet_api.payment.dto.GatewayCallbackPayload;
import com.novawallet.novawallet_api.payment.dto.GatewayInitiationResponse;
import com.novawallet.novawallet_api.payment.dto.GatewayPaymentRequest;
import com.novawallet.novawallet_api.payment.dto.GatewayStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 * Lipila Zambia implementation of {@link PaymentGateway}.
 *
 * <p>REST contract (docs.lipila.dev):</p>
 * <ul>
 *   <li>{@code POST {base-url}/collections/mobile-money} — initiate a collection.</li>
 *   <li>{@code POST {base-url}/disbursements/mobile-money} — initiate a payout
 *       (additionally requires the merchant egress IP to be whitelisted with
 *       Lipila server-side).</li>
 *   <li>Auth: {@code x-api-key} request header; an optional
 *       {@code callbackUrl} header tells Lipila where to POST the async
 *       callback JSON ({@link GatewayCallbackPayload} shape).</li>
 *   <li>Body: {@code referenceId}, {@code amount}, {@code narration},
 *       {@code accountNumber}, {@code currency} (ZMW).</li>
 * </ul>
 *
 * <p>Timeouts are deliberately configured (connect + read) so a hung provider
 * cannot pin servlet/DB threads indefinitely.</p>
 */
@Component
public class LipilaGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(LipilaGateway.class);

    /** Connection-establishment timeout for gateway calls. */
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    /** Per-request read timeout for gateway calls. */
    static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    /** Header carrying the merchant API key. */
    static final String API_KEY_HEADER = "x-api-key";

    /** Optional header telling Lipila where to deliver async callbacks. */
    static final String CALLBACK_URL_HEADER = "callbackUrl";

    private final RestClient restClient;
    private final String callbackUrl;

    @Autowired
    public LipilaGateway(RestClient.Builder restClientBuilder, PaymentConfig config) {
        this(buildFrom(withTimeoutFactory(restClientBuilder), config), config.getCallbackUrl());
    }

    private LipilaGateway(RestClient restClient, String callbackUrl) {
        this.restClient = restClient;
        this.callbackUrl = callbackUrl;
    }

    /**
     * Test factory: identical request-building logic (base URL + auth header)
     * but WITHOUT forcing the timeout request factory, so a
     * {@code MockRestServiceServer} bound to the builder beforehand survives.
     */
    static LipilaGateway forTesting(RestClient.Builder builder, PaymentConfig config) {
        return new LipilaGateway(buildFrom(builder, config), config.getCallbackUrl());
    }

    private static RestClient.Builder withTimeoutFactory(RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return builder.requestFactory(requestFactory);
    }

    private static RestClient buildFrom(RestClient.Builder builder, PaymentConfig config) {
        return builder
                .baseUrl(config.getBaseUrl())
                .defaultHeader(API_KEY_HEADER, config.getApiKey())
                .build();
    }

    @Override
    public GatewayInitiationResponse initiateCollection(GatewayPaymentRequest request) {
        log.info("Lipila collection initiated: reference={}, amount={}, account={}",
                request.referenceId(), request.amount(), maskAccount(request.accountNumber()));
        return post("/collections/mobile-money", request);
    }

    @Override
    public GatewayInitiationResponse initiateDisbursement(GatewayPaymentRequest request) {
        log.info("Lipila disbursement initiated: reference={}, amount={}, account={}",
                request.referenceId(), request.amount(), maskAccount(request.accountNumber()));
        return post("/disbursements/mobile-money", request);
    }

    @Override
    public GatewayStatusResponse checkStatus(String referenceId) {
        GatewayCallbackPayload payload = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/transactions/{referenceId}/status").build(referenceId))
                .retrieve()
                .body(GatewayCallbackPayload.class);

        if (payload == null) {
            throw new IllegalStateException("Lipila returned an empty status body for " + referenceId);
        }
        return new GatewayStatusResponse(
                payload.referenceId() != null ? payload.referenceId() : referenceId,
                payload.status(),
                payload.message()
        );
    }

    // ==================== Helpers ====================

    private GatewayInitiationResponse post(String path, GatewayPaymentRequest request) {
        Map<String, Object> body = Map.of(
                "referenceId", request.referenceId(),
                "amount", request.amount(),
                "narration", request.narration(),
                "accountNumber", request.accountNumber(),
                "currency", request.currency()
        );

        GatewayCallbackPayload response = restClient.post()
                .uri(path)
                .headers(headers -> {
                    if (callbackUrl != null && !callbackUrl.isBlank()) {
                        headers.set(CALLBACK_URL_HEADER, callbackUrl);
                    }
                })
                .body(body)
                .retrieve()
                .body(GatewayCallbackPayload.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Lipila returned an empty initiation body for " + request.referenceId());
        }

        log.info("Lipila initiation accepted: reference={}, status={}",
                request.referenceId(), response.status());

        return new GatewayInitiationResponse(
                response.referenceId() != null ? response.referenceId() : request.referenceId(),
                response.status(),
                response.message()
        );
    }

    /** Never log full MSISDNs — mask everything but the last three digits. */
    private static String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 3) {
            return "***";
        }
        return "***" + accountNumber.substring(accountNumber.length() - 3);
    }
}
