package com.lootsafe.payment.service;

import com.lootsafe.enums.WebhookEventStatus;
import com.lootsafe.repository.PaymentWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookProcessorService {

    private static final String LOG_PROCESSING_FAILURE =
            "Falha ao processar a notificação da ordem {}.";

    private final WebhookReconciliationService webhookReconciliationService;
    private final PaymentWebhookEventRepository webhookEventRepository;

    @Async("webhookExecutor")
    public void processOrder(String orderId, UUID eventId) {
        try {
            webhookReconciliationService.reconcileOrder(orderId);
            updateEventStatus(eventId, WebhookEventStatus.PROCESSED);
        } catch (RuntimeException ex) {
            log.error(LOG_PROCESSING_FAILURE, orderId, ex);
            updateEventStatus(eventId, WebhookEventStatus.FAILED);
        }
    }

    private void updateEventStatus(UUID eventId, WebhookEventStatus status) {
        webhookEventRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(status);
            webhookEventRepository.save(event);
        });
    }
}