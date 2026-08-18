package com.lootsafe.repository;

import com.lootsafe.entity.PaymentWebhookEvent;
import com.lootsafe.enums.WebhookEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {

    Optional<PaymentWebhookEvent> findByExternalEventId(String externalEventId);

    List<PaymentWebhookEvent> findByStatus(WebhookEventStatus status);
}