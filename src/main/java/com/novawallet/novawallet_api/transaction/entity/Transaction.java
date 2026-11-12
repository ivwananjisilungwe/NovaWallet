package com.novawallet.novawallet_api.transaction.entity;

import com.novawallet.novawallet_api.transaction.enums.TransactionStatus;
import com.novawallet.novawallet_api.transaction.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Maps to the {@code transactions} table. Immutable record of every financial operation.
 *
 * <p>Transactions are created for deposits, withdrawals, transfers, and fees. Each record
 * captures the balance before and after the operation (ledger-style). Transfer pairs
 * (DEBIT + CREDIT) are linked via {@code relatedTransactionId}. A human-readable
 * reference number ({@code NWTXyyyyMMdd00000001}) is generated for each record.</p>
 */
@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.SUCCESSFUL;

    @Column(length = 255)
    private String description;

    // Dedicated payout data (Batch 3 B3.1) — populated for WITHDRAWAL payouts.
    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Column(name = "external_status", length = 50)
    private String externalStatus;

    @Column(name = "payout_account", length = 255)
    private String payoutAccount;

    @Column(name = "payout_channel", length = 50)
    private String payoutChannel;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "next_status_check_at")
    private LocalDateTime nextStatusCheckAt;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "sender_wallet_id")
    private UUID senderWalletId;

    @Column(name = "receiver_wallet_id")
    private UUID receiverWalletId;

    @Column(name = "related_transaction_id")
    private UUID relatedTransactionId;

    @Version
    private Integer version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = TransactionStatus.SUCCESSFUL;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
