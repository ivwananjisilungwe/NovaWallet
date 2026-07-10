package com.novawallet.novawallet_api.fee.entity;

import com.novawallet.novawallet_api.fee.enums.FeeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fee_configurations")
public class FeeConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, unique = true, length = 20)
    private FeeType transactionType;

    @NotNull
    @DecimalMin("0.0000")
    @Column(name = "percentage_fee", nullable = false, precision = 5, scale = 4)
    private BigDecimal percentageFee;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "flat_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal flatFee;

    @DecimalMin("0.00")
    @Column(name = "min_fee", precision = 12, scale = 2)
    private BigDecimal minFee;

    @DecimalMin("0.00")
    @Column(name = "max_fee", precision = 12, scale = 2)
    private BigDecimal maxFee;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public FeeConfiguration() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== Builder ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private FeeType transactionType;
        private BigDecimal percentageFee = BigDecimal.ZERO;
        private BigDecimal flatFee = BigDecimal.ZERO;
        private BigDecimal minFee;
        private BigDecimal maxFee;
        private boolean active = true;

        public Builder transactionType(FeeType transactionType) { this.transactionType = transactionType; return this; }
        public Builder percentageFee(BigDecimal percentageFee) { this.percentageFee = percentageFee; return this; }
        public Builder flatFee(BigDecimal flatFee) { this.flatFee = flatFee; return this; }
        public Builder minFee(BigDecimal minFee) { this.minFee = minFee; return this; }
        public Builder maxFee(BigDecimal maxFee) { this.maxFee = maxFee; return this; }
        public Builder active(boolean active) { this.active = active; return this; }

        public FeeConfiguration build() {
            FeeConfiguration fc = new FeeConfiguration();
            fc.transactionType = transactionType;
            fc.percentageFee = percentageFee;
            fc.flatFee = flatFee;
            fc.minFee = minFee;
            fc.maxFee = maxFee;
            fc.active = active;
            return fc;
        }
    }

    // ==================== Getters & Setters ====================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public FeeType getTransactionType() { return transactionType; }
    public void setTransactionType(FeeType transactionType) { this.transactionType = transactionType; }
    public BigDecimal getPercentageFee() { return percentageFee; }
    public void setPercentageFee(BigDecimal percentageFee) { this.percentageFee = percentageFee; }
    public BigDecimal getFlatFee() { return flatFee; }
    public void setFlatFee(BigDecimal flatFee) { this.flatFee = flatFee; }
    public BigDecimal getMinFee() { return minFee; }
    public void setMinFee(BigDecimal minFee) { this.minFee = minFee; }
    public BigDecimal getMaxFee() { return maxFee; }
    public void setMaxFee(BigDecimal maxFee) { this.maxFee = maxFee; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
