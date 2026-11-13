package com.novawallet.novawallet_api.payment.repository;

import com.novawallet.novawallet_api.payment.entity.ProviderWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderWebhookEventRepository extends JpaRepository<ProviderWebhookEvent, UUID> {

    Optional<ProviderWebhookEvent> findByProviderAndEventId(String provider, String eventId);
}
