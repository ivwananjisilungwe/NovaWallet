package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.wallet.entity.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for per-user velocity limits on money-out flows
 * (withdrawals and transfers): rolling-hour COUNT cap and AMOUNT cap,
 * enforced before PIN verification and layered beneath the KYC daily-send-limit.
 *
 * <p>Thresholds are tightened for this test context only via
 * {@link TestPropertySource} (count = 2/hour, amount = 100.00/hour) so the
 * limits can be reached quickly without touching other tests' contexts.</p>
 */
@TestPropertySource(properties = {
        "app.transaction.velocity.max-outgoing-count-per-hour=2",
        "app.transaction.velocity.max-outgoing-amount-per-hour=100"
})
class TransactionVelocityLimitIntegrationTest extends EndpointTestSupport {

    private static final String PIN = "1234";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Withdrawal blocked once hourly outgoing count limit is reached")
    void withdraw_countLimitReached_shouldReturnTooManyRequests() throws Exception {
        RegisteredUser user = registerEndpointUser("velcount");
        verifyEmailFor(user);
        Wallet wallet = fundedWallet(user, "100.00");
        setPin(user);

        withdraw(user, wallet, "5.00").andExpect(status().isCreated());
        withdraw(user, wallet, "5.00").andExpect(status().isCreated());

        // Third outgoing within the rolling hour exceeds the count cap (2)
        withdraw(user, wallet, "5.00")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.message", containsString("Too many outgoing transactions")));
    }

    @Test
    @DisplayName("Transfer blocked when projected hourly outgoing amount exceeds limit")
    void transfer_amountLimitExceeded_shouldReturnTooManyRequests() throws Exception {
        RegisteredUser sender = registerEndpointUser("velamount");
        RegisteredUser receiver = registerEndpointUser("velreceiver");
        verifyEmailFor(sender);
        Wallet senderWallet = fundedWallet(sender, "300.00");
        Wallet receiverWallet = createWalletFor(receiver);
        setPin(sender);

        // First transfer: 60.00 sent ≤ 100.00 hourly amount cap → allowed
        transfer(sender, senderWallet, receiverWallet, "60.00").andExpect(status().isCreated());

        // Second transfer projects 60.00 + 60.00 = 120.00 > 100.00 → blocked by amount cap
        // (count is still 1 < 2, proving the amount rule fires independently)
        transfer(sender, senderWallet, receiverWallet, "60.00")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.message", containsString("Hourly outgoing amount limit")));
    }

    @Test
    @DisplayName("Outgoing allowed again after the rolling-hour window expires")
    void withdraw_windowExpired_shouldBeAllowedAgain() throws Exception {
        RegisteredUser user = registerEndpointUser("velwindow");
        verifyEmailFor(user);
        Wallet wallet = fundedWallet(user, "100.00");
        setPin(user);

        withdraw(user, wallet, "5.00").andExpect(status().isCreated());
        withdraw(user, wallet, "5.00").andExpect(status().isCreated());

        // Count cap reached → blocked while inside the window
        withdraw(user, wallet, "5.00").andExpect(status().isTooManyRequests());

        // Age all of the user's outgoing transactions beyond the 60-minute window
        jdbcTemplate.update(
                "UPDATE transactions SET created_at = ? WHERE sender_wallet_id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(90)),
                wallet.getId());

        // Window expired → the same withdrawal is allowed again
        withdraw(user, wallet, "5.00").andExpect(status().isCreated());
    }

    // ==================== Helpers ====================

    /** Creates a wallet for the user pre-funded with the given balance. */
    private Wallet fundedWallet(RegisteredUser user, String balance) {
        Wallet wallet = createWalletFor(user);
        wallet.setBalance(new BigDecimal(balance));
        return walletRepository.save(wallet);
    }

    /** Sets the transaction PIN ("1234") required by withdraw/transfer. */
    private void setPin(RegisteredUser user) throws Exception {
        mockMvc.perform(post("/api/v1/pin")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"" + PIN + "\"}"))
                .andExpect(status().isOk());
    }

    private ResultActions withdraw(RegisteredUser user, Wallet wallet, String amount) throws Exception {
        return mockMvc.perform(post("/api/v1/wallets/" + wallet.getId() + "/withdraw")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount + ",\"pin\":\"" + PIN
                                + "\",\"description\":\"Velocity test\"}"));
    }

    private ResultActions transfer(RegisteredUser sender, Wallet senderWallet,
                                   Wallet receiverWallet, String amount) throws Exception {
        return mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverWalletId\":\"" + receiverWallet.getId()
                                + "\",\"amount\":" + amount + ",\"pin\":\"" + PIN
                                + "\",\"description\":\"Velocity test\"}"));
    }
}
