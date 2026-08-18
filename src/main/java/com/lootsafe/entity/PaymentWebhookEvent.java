package com.lootsafe.entity;

import com.lootsafe.enums.WebhookEventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_webhook_events")
public class PaymentWebhookEvent extends AbstractAuditableEntity {

    @Column(name = "external_event_id")
    private String externalEventId;

    private String type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookEventStatus status;
}