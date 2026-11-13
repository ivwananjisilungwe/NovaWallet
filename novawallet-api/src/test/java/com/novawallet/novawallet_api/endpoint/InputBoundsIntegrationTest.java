package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.exception.ErrorResponse;
import com.novawallet.novawallet_api.exception.GlobalExceptionHandler;
import com.novawallet.novawallet_api.user.entity.Role;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1C Batch 2 — boundary and mass-assignment checks for request input.
 *
 * <p>Each case is sent as an authenticated customer (or admin where the route is
 * admin-only). Validation failures are expected before any service call, so the
 * wallets used here are not required to hold funds.</p>
 */
class InputBoundsIntegrationTest extends EndpointTestSupport {

    @Value("${spring.servlet.multipart.max-file-size}")
    private String multipartMaxFileSize;

    private RegisteredUser customer;
    private Wallet customerWallet;

    @BeforeEach
    void createCustomer() throws Exception {
        customer = registerEndpointUser("bounds");
        verifyEmailFor(customer);
        customerWallet = createWalletFor(customer);
    }

    private ResultActions postAsCustomer(String path, String json) throws Exception {
        return mockMvc.perform(post(path)
                .header("Authorization", "Bearer " + customer.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String transferJson(String amount, String pin, String extraField) {
        String extra = extraField == null ? "" : "," + extraField;
        return "{\"receiverWalletId\":\"" + UUID.randomUUID() + "\","
                + "\"amount\":" + amount + ","
                + "\"pin\":\"" + pin + "\","
                + "\"description\":\"bounds test\"" + extra + "}";
    }

    private String withdrawJson(String amount, String pin) {
        return "{\"amount\":" + amount + ",\"pin\":\"" + pin + "\",\"description\":\"bounds test\"}";
    }

    @Nested
    @DisplayName("money amounts")
    class Amounts {

        @Test
        @DisplayName("transfer amount 0.001 (sub-minor-unit) is 400")
        void transfer_subMinorUnitAmount_isBadRequest() throws Exception {
            postAsCustomer("/api/v1/transfers", transferJson("0.001", "1234", null))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("transfer amount 10000000000000.00 (above 13 integer digits) is 400")
        void transfer_amountAboveMaximum_isBadRequest() throws Exception {
            postAsCustomer("/api/v1/transfers", transferJson("10000000000000.00", "1234", null))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("withdraw amount 0.001 is 400")
        void withdraw_subMinorUnitAmount_isBadRequest() throws Exception {
            postAsCustomer("/api/v1/wallets/" + customerWallet.getId() + "/withdraw",
                    withdrawJson("0.001", "1234"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("withdraw amount 10000000000000.00 is 400")
        void withdraw_amountAboveMaximum_isBadRequest() throws Exception {
            postAsCustomer("/api/v1/wallets/" + customerWallet.getId() + "/withdraw",
                    withdrawJson("10000000000000.00", "1234"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("fee estimate amount 1.001 (3 decimal places) is 400")
        void feeEstimate_threeDecimalPlaces_isBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/fees/estimate")
                            .param("type", "TRANSFER")
                            .param("amount", "1.001")
                            .header("Authorization", "Bearer " + customer.token()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("transaction PIN")
    class Pin {

        @Test
        @DisplayName("PIN with a non-digit (12a4) is 400 on transfer")
        void transfer_nonDigitPin_isBadRequest() throws Exception {
            postAsCustomer("/api/v1/transfers", transferJson("10.00", "12a4", null))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PIN with a non-digit (12a4) is 400 on withdraw")
        void withdraw_nonDigitPin_isBadRequest() throws Exception {
            postAsCustomer("/api/v1/wallets/" + customerWallet.getId() + "/withdraw",
                    withdrawJson("10.00", "12a4"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("payout destination")
    class Payout {

        @Test
        @DisplayName("payout MSISDN 'abc' is 400")
        void payout_nonNumericMsisdn_isBadRequest() throws Exception {
            postAsCustomer("/api/v1/wallets/" + customerWallet.getId() + "/payouts",
                    "{\"amount\":10.00,\"pin\":\"1234\",\"accountNumber\":\"abc\",\"description\":\"bounds\"}")
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("paging bounds (PageRequests: page >= 0, 1 <= size <= 100)")
    class Paging {

        private String historyPath(String query) {
            return "/api/v1/wallets/" + customerWallet.getId() + "/transactions" + query;
        }

        @Test
        @DisplayName("size=101 is 400")
        void sizeAboveMaximum_isBadRequest() throws Exception {
            mockMvc.perform(get(historyPath("?size=101"))
                            .header("Authorization", "Bearer " + customer.token()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("size=0 is 400")
        void sizeZero_isBadRequest() throws Exception {
            mockMvc.perform(get(historyPath("?size=0"))
                            .header("Authorization", "Bearer " + customer.token()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("page=-1 is 400")
        void negativePage_isBadRequest() throws Exception {
            mockMvc.perform(get(historyPath("?page=-1"))
                            .header("Authorization", "Bearer " + customer.token()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("size=100 (maximum) is 200")
        void sizeAtMaximum_isOk() throws Exception {
            mockMvc.perform(get(historyPath("?size=100"))
                            .header("Authorization", "Bearer " + customer.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("unknown JSON fields are rejected (mass assignment, F5/F9)")
    class UnknownFields {

        @Test
        @DisplayName("PUT /users/me with role=ADMIN is 400 and the role is not changed")
        void updateProfile_roleField_isBadRequest() throws Exception {
            mockMvc.perform(put("/api/v1/users/me")
                            .header("Authorization", "Bearer " + customer.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\":\"Renamed\",\"role\":\"ADMIN\"}"))
                    .andExpect(status().isBadRequest());

            Role role = endpointUserRepository.findById(customer.userId()).orElseThrow().getRole();
            assertThat(role).isNotEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("transfer with balance=1 is 400")
        void transfer_balanceField_isBadRequest() throws Exception {
            postAsCustomer("/api/v1/transfers", transferJson("10.00", "1234", "\"balance\":1"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("admin inputs")
    class AdminInputs {

        @Test
        @DisplayName("CreateFee percentageFee 1.5 (above 100%) is 400 as admin")
        void createFee_percentageAboveOne_isBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/admin/fees")
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transactionType\":\"TRANSFER\",\"percentageFee\":1.5,"
                                    + "\"flatFee\":2.00,\"minFee\":1.00,\"maxFee\":50.00}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("KYC approve with tier 99 is 400 and the submission stays PENDING")
        void approveKyc_unknownTier_isBadRequest() throws Exception {
            RegisteredUser applicant = registerEndpointUser("kyctier");
            uploadKycDocument(applicant);
            mockMvc.perform(post("/api/v1/kyc/submit")
                            .header("Authorization", "Bearer " + applicant.token()))
                    .andExpect(status().isOk());

            String adminToken = adminToken();
            mockMvc.perform(post("/api/v1/admin/kyc/" + applicant.userId() + "/approve")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tier\":99}"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(get("/api/v1/admin/kyc/" + applicant.userId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.kycStatus").value("PENDING"));
        }
    }

    @Nested
    @DisplayName("multipart upload size")
    class MultipartLimit {

        /**
         * MockMvc does not enforce multipart size limits: MockMultipartFile bypasses the
         * servlet container's max-file-size check that Tomcat applies to real requests.
         * So this test asserts the configured limit and the exception mapping instead of
         * sending an oversized upload through MockMvc.
         */
        @Test
        @DisplayName("max-file-size is 10MB and MaxUploadSizeExceededException maps to 413")
        void configuredLimitAndHandlerMapping() {
            assertThat(DataSize.parse(multipartMaxFileSize)).isEqualTo(DataSize.ofMegabytes(10));

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/kyc/documents/upload");
            ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
                    .handleUploadTooLarge(new MaxUploadSizeExceededException(DataSize.ofMegabytes(10).toBytes()), request);

            assertThat(response.getStatusCode().value()).isEqualTo(413);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("PAYLOAD_TOO_LARGE");
        }
    }
}
