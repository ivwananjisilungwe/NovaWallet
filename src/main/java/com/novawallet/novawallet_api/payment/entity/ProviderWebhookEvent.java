package com.novawallet.novawallet_api.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Durable record of an inbound provider webhook event (Batch 3 B3.3).
 *
 * <p>The {@code (provider, event_id)} unique constraint is the durable dedup:
 * a duplicate delivery of the same event is rejected at INSERT time and never
 * produces a second financial effect. The event's lifecycle is captured in
 * {@link #processingStatus}.</p>
 */
@Entity
@Table(name = "provider_webhook_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @Column(name = "reference_id", length = 255)
    private String referenceId;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "signature_ok", nullable = false)
    @Builder.Default
    private boolean signatureOk = false;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    @Builder.Default
    private ProviderWebhookEventStatus processingStatus = ProviderWebhookEventStatus.RECEIVED;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}
