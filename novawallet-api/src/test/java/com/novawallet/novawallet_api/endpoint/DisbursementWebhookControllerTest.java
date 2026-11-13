package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.endpoint.EndpointTestSupport;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F3 correction tests: malformed disbursement-webhook JSON must produce HTTP 400
 * (never 500), must never reach signature verification or payout finalization,
 * and valid deliveries continue through the normal HMAC-gated pipeline.
 *
 * <p>The endpoint consumes the raw body (required for HMAC verification), so
 * parsing happens manually in the controller; these tests assert the documented
 * 400/401/200 contract end-to-end through the real servlet stack.</p>
 */
@TestPropertySource(properties = {
        "app.payments.lipila.enabled=true",
        "app.payments.lipila.webhook-secret=9hXwJfvyJqx7Oslku3FDvn8tRrV3+RZ8achVCXWTbQI="
})
class DisbursementWebhookControllerTest extends EndpointTestSupport {

    private static final String SECRET = "9hXwJfvyJqx7Oslku3FDvn8tRrV3+RZ8achVCXWTbQI=";
    private static final String ENDPOINT = "/api/v1/webhooks/lipila/disbursements";

    @Autowired
    private TransactionRepository transactionRepository;

    private String nowTimestamp() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    /** Lipila Standard Webhooks signature over {@code id.timestamp.rawBody}. */
    private String sign(String eventId, String timestamp, String rawBody) {
        try {
            byte[] key = Base64.getDecoder().decode(SECRET);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] bytes = mac.doFinal(
                    (eventId + "." + timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8));
            return "v1," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long payoutsInDb() {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getProviderReference() != null)
                .count();
    }

    @Test
    @DisplayName("F3: malformed JSON body returns 400 and mutates nothing")
    void malformedJson_returns400_noStateChange() throws Exception {
        long before = payoutsInDb();

        mockMvc.perform(post(ENDPOINT)
                        .header("webhook-id", "evt-ctl-malformed")
                        .header("webhook-timestamp", nowTimestamp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        assertThatPayoutsUnchanged(before);
    }

    @Test
    @DisplayName("F3: empty request body returns 400")
    void emptyBody_returns400() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header("webhook-id", "evt-ctl-empty")
                        .header("webhook-timestamp", nowTimestamp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("F3: structurally invalid JSON (wrong type) returns 400")
    void invalidJsonStructure_returns400() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header("webhook-id", "evt-ctl-structure")
                        .header("webhook-timestamp", nowTimestamp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Valid JSON with an invalid HMAC signature returns 401")
    void validJson_invalidSignature_returns401() throws Exception {
        String ts = nowTimestamp();
        mockMvc.perform(post(ENDPOINT)
                        .header("webhook-id", "evt-ctl-badsig")
                        .header("webhook-timestamp", ts)
                        .header("webhook-signature", "v1,not-a-real-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"referenceId":"NW:ctl-unknown","status":"Successful","amount":100.00}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Valid JSON with a valid HMAC signature is acknowledged (200)")
    void validJson_validSignature_returns200() throws Exception {
        String ts = nowTimestamp();
        String body = """
                {"referenceId":"NW:ctl-unknown","status":"Successful","amount":100.00,
                 "accountNumber":"260961234567","message":"ok"}
                """;
        String signature = sign("evt-ctl-good", ts, body);
        long before = payoutsInDb();

        mockMvc.perform(post(ENDPOINT)
                        .header("webhook-id", "evt-ctl-good")
                        .header("webhook-timestamp", ts)
                        .header("webhook-signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                // Unknown reference is quarantined and acknowledged with 200 —
                // no payout exists to finalize and none may be created.
                .andExpect(status().isOk());

        assertThatPayoutsUnchanged(before);
    }

    private void assertThatPayoutsUnchanged(long before) {
        org.assertj.core.api.Assertions.assertThat(payoutsInDb())
                .as("Malformed or unverifiable callbacks must never create payout rows")
                .isEqualTo(before);
    }
}
