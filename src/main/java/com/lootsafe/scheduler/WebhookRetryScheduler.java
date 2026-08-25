package com.lootsafe.scheduler;

import com.lootsafe.entity.PaymentWebhookEvent;
import com.lootsafe.enums.WebhookEventStatus;
import com.lootsafe.payment.service.WebhookProcessorService;
import com.lootsafe.repository.PaymentWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRetryScheduler {

    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final WebhookProcessorService webhookProcessorService;

    @Scheduled(fixedDelayString = "${payment.webhook-retry-interval-ms:300000}")
    @SchedulerLock(name = "retryFailedWebhooksLock", lockAtLeastFor = "30s", lockAtMostFor = "10m")
    public void retryFailedWebhooks() {

        List<PaymentWebhookEvent> failedEvents = paymentWebhookEventRepository
                .findTop50ByStatusOrderByCreatedAtAsc(WebhookEventStatus.FAILED);

        if (failedEvents.isEmpty()) {
            return;
        }

        log.info("Encontrados {} eventos de webhook com falha. Iniciando reprocessamento...", failedEvents.size());

        for (PaymentWebhookEvent event : failedEvents) {
            log.info("Reprocessando evento: {}", event.getId());

            try {
                webhookProcessorService.processOrder(event.getExternalEventId(), event.getId());
            } catch (Exception e) {
                log.error("Erro ao reprocessar o evento {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}