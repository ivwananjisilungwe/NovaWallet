package com.novawallet.novawallet_api.transaction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Velocity-limit thresholds for money-out flows (withdrawals and transfers).
 *
 * <p>Anti-abuse guardrails enforced per-user over a rolling 60-minute window,
 * layered on top of the KYC daily-send-limit (which is tier-gated). Unlike the
 * KYC limits, velocity limits apply to <em>all</em> users so that even
 * email-verified accounts without an approved KYC tier have a send cap.</p>
 *
 * <p>Follows the {@code app.kyc} {@code @ConfigurationProperties} precedent:
 * thresholds are static configuration rather than DB-backed admin CRUD
 * (the fee-configuration table's schema is fee-specific and does not fit).</p>
 */
@Component
@ConfigurationProperties(prefix = "app.transaction.velocity")
public class TransactionVelocityConfig {

    /** Maximum number of outgoing transactions (withdrawals + transfers) per rolling hour. */
    private int maxOutgoingCountPerHour = 10;

    /** Maximum total outgoing amount (withdrawals + transfers, fees excluded) per rolling hour. */
    private BigDecimal maxOutgoingAmountPerHour = new BigDecimal("50000.00");

    public int getMaxOutgoingCountPerHour() {
        return maxOutgoingCountPerHour;
    }

    public void setMaxOutgoingCountPerHour(int maxOutgoingCountPerHour) {
        this.maxOutgoingCountPerHour = maxOutgoingCountPerHour;
    }

    public BigDecimal getMaxOutgoingAmountPerHour() {
        return maxOutgoingAmountPerHour;
    }

    public void setMaxOutgoingAmountPerHour(BigDecimal maxOutgoingAmountPerHour) {
        this.maxOutgoingAmountPerHour = maxOutgoingAmountPerHour;
    }
}
