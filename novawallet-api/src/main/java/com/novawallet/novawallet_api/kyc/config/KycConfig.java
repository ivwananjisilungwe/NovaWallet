package com.novawallet.novawallet_api.kyc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.kyc")
public class KycConfig {

    private Map<Integer, TierConfig> tiers;

    public Map<Integer, TierConfig> getTiers() {
        return tiers;
    }

    public void setTiers(Map<Integer, TierConfig> tiers) {
        this.tiers = tiers;
    }

    public TierConfig getTier(int tierLevel) {
        TierConfig config = tiers.get(tierLevel);
        if (config == null) {
            throw new IllegalArgumentException("Invalid KYC tier: " + tierLevel);
        }
        return config;
    }

    public static class TierConfig {
        private String name;
        private BigDecimal walletLimit;
        private BigDecimal dailySendLimit;
        private List<String> requiredDocuments;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public BigDecimal getWalletLimit() { return walletLimit; }
        public void setWalletLimit(BigDecimal walletLimit) { this.walletLimit = walletLimit; }

        public BigDecimal getDailySendLimit() { return dailySendLimit; }
        public void setDailySendLimit(BigDecimal dailySendLimit) { this.dailySendLimit = dailySendLimit; }

        public List<String> getRequiredDocuments() { return requiredDocuments; }
        public void setRequiredDocuments(List<String> requiredDocuments) { this.requiredDocuments = requiredDocuments; }
    }
}
