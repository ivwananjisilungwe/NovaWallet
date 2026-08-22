package com.novawallet.novawallet_api.payment;

import com.novawallet.novawallet_api.payment.config.PaymentConfig;
import com.novawallet.novawallet_api.payment.dto.GatewayInitiationResponse;
import com.novawallet.novawallet_api.payment.dto.GatewayPaymentRequest;
import com.novawallet.novawallet_api.payment.dto.GatewayStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link LipilaGateway} request building and response parsing.
 * Uses {@link MockRestServiceServer#bindTo(RestClient.Builder)} so the exact
 * production code path ({@code forTesting} shares {@code buildFrom}) is
 * exercised without network I/O.
 */
class LipilaGatewayTest {

    private static final String BASE_URL = "https://api.lipila.dev/api/v1";
    private static final String API_KEY = "lipila-test-key";

    private LipilaGateway gateway;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        PaymentConfig config = new PaymentConfig();
        config.setBaseUrl(BASE_URL);
        config.setApiKey(API_KEY);

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = LipilaGateway.forTesting(builder, config);
    }

    @Test
    @DisplayName("initiateCollection posts the documented contract to /collections/mobile-money")
    void initiateCollection_buildsDocumentedRequest() {
        GatewayPaymentRequest request = new GatewayPaymentRequest(
                "NW:3f2c9a1e:8b1d", new BigDecimal("150.00"),
                "wallet top-up", "260961234567", "ZMW");

        server.expect(requestTo(BASE_URL + "/collections/mobile-money"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(LipilaGateway.API_KEY_HEADER, API_KEY))
                .andExpect(jsonPath("$.referenceId").value("NW:3f2c9a1e:8b1d"))
                .andExpect(jsonPath("$.amount").value(150.0))
                .andExpect(jsonPath("$.narration").value("wallet top-up"))
                .andExpect(jsonPath("$.accountNumber").value("260961234567"))
                .andExpect(jsonPath("$.currency").value("ZMW"))
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"referenceId\":\"NW:3f2c9a1e:8b1d\","
                                + "\"status\":\"Pending\",\"message\":\"submitted\"}"));

        GatewayInitiationResponse response = gateway.initiateCollection(request);

        assertThat(response.referenceId()).isEqualTo("NW:3f2c9a1e:8b1d");
        assertThat(response.status()).isEqualTo("Pending");
        assertThat(response.message()).isEqualTo("submitted");
        server.verify();
    }

    @Test
    @DisplayName("initiateDisbursement posts to /disbursements/mobile-money")
    void initiateDisbursement_targetsDisbursementsPath() {
        GatewayPaymentRequest request = new GatewayPaymentRequest(
                "NW:3f2c9a1e:99aa", new BigDecimal("75.50"),
                "payout", "260977778888", "ZMW");

        server.expect(requestTo(BASE_URL + "/disbursements/mobile-money"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(LipilaGateway.API_KEY_HEADER, API_KEY))
                .andExpect(jsonPath("$.referenceId").value("NW:3f2c9a1e:99aa"))
                .andExpect(jsonPath("$.amount").value(75.5))
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"Pending\",\"message\":\"queued\"}"));

        GatewayInitiationResponse response = gateway.initiateDisbursement(request);

        // Provider omitted referenceId → our merchant reference is echoed back
        assertThat(response.referenceId()).isEqualTo("NW:3f2c9a1e:99aa");
        assertThat(response.status()).isEqualTo("Pending");
        server.verify();
    }

    @Test
    @DisplayName("callbackUrl header is sent only when configured")
    void callbackUrlHeader_sentOnlyWhenConfigured() {
        PaymentConfig config = new PaymentConfig();
        config.setBaseUrl(BASE_URL);
        config.setApiKey(API_KEY);
        config.setCallbackUrl("https://api.novawallet.com/api/v1/webhooks/lipila/collections");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer boundServer = MockRestServiceServer.bindTo(builder).build();
        LipilaGateway gatewayWithCallback = LipilaGateway.forTesting(builder, config);

        boundServer.expect(requestTo(BASE_URL + "/collections/mobile-money"))
                .andExpect(header(
                        LipilaGateway.CALLBACK_URL_HEADER,
                        "https://api.novawallet.com/api/v1/webhooks/lipila/collections"))
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"Pending\"}"));

        gatewayWithCallback.initiateCollection(new GatewayPaymentRequest(
                "ref-1", BigDecimal.ONE, "n", "260960000000", "ZMW"));

        boundServer.verify();

        // And the default-configured gateway must NOT send the header
        server.expect(request -> assertThat(
                        request.getHeaders().get(LipilaGateway.CALLBACK_URL_HEADER))
                .as("callbackUrl header must be absent when not configured")
                .isNull())
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"Pending\"}"));

        gateway.initiateCollection(new GatewayPaymentRequest(
                "ref-2", BigDecimal.ONE, "n", "260960000000", "ZMW"));

        server.verify();
    }

    @Test
    @DisplayName("checkStatus queries the provider status endpoint")
    void checkStatus_queriesStatusEndpoint() {
        // Match on the decoded path so the assertion is independent of how
        // Spring encodes ':' inside the path segment.
        server.expect(request -> assertThat(request.getURI().getPath())
                .isEqualTo("/api/v1/transactions/NW:abc:def/status"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(LipilaGateway.API_KEY_HEADER, API_KEY))
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"referenceId\":\"NW:abc:def\","
                                + "\"status\":\"Successful\",\"message\":\"done\"}"));

        GatewayStatusResponse response = gateway.checkStatus("NW:abc:def");

        assertThat(response.status()).isEqualTo("Successful");
        assertThat(response.message()).isEqualTo("done");
        server.verify();
    }
}
