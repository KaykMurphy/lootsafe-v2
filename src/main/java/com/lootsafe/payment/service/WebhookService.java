package com.lootsafe.payment.service;

import com.lootsafe.config.MercadoPagoProperties;
import com.lootsafe.entity.PaymentWebhookEvent;
import com.lootsafe.enums.WebhookEventStatus;
import com.lootsafe.repository.PaymentWebhookEventRepository;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.webhook.WebhookSignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final Duration SIGNATURE_TOLERANCE = Duration.ofMinutes(5);
    private static final String LOG_WEBHOOK_WITHOUT_ORDER = "Webhook recebido sem identificador de ordem.";
    private static final String LOG_WEBHOOK_ALREADY_EXISTS = "Webhook já recebido anteriormente para o evento {}. Ignorando duplicidade.";
    private static final String LOG_CONCURRENT_WEBHOOK_IGNORED = "Concorrência detectada ao salvar evento {} de webhook. Ignorando duplicidade.";
    private static final String LOG_SIGNATURE_INVALID = "Assinatura do webhook rejeitada (reason={}, requestId={}).";

    private final MercadoPagoProperties mercadopagoProperties;
    private final WebhookProcessorService webhookProcessorService;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;

    public void receiveNotification(String dataId, String xSignature, String xRequestId)
            throws MPInvalidWebhookSignatureException {
        validateSignature(xSignature, xRequestId, dataId);

        if (dataId == null || dataId.isBlank()) {
            log.warn(LOG_WEBHOOK_WITHOUT_ORDER);
            return;
        }

        if (paymentWebhookEventRepository.existsByExternalEventId(dataId)) {
            log.info(LOG_WEBHOOK_ALREADY_EXISTS, dataId);
            return;
        }

        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setExternalEventId(dataId);
        event.setType("mercadopago.order");
        event.setPayload("{\"data\":{\"id\":\"" + dataId + "\"}}");
        event.setStatus(WebhookEventStatus.RECEIVED);

        PaymentWebhookEvent savedEvent;
        try {
            savedEvent = paymentWebhookEventRepository.save(event);
        } catch (DataIntegrityViolationException ex) {
            log.warn(LOG_CONCURRENT_WEBHOOK_IGNORED, dataId);
            return;
        }

        webhookProcessorService.processOrder(dataId, savedEvent.getId());
    }

    private void validateSignature(String xSignature, String xRequestId, String dataId)
            throws MPInvalidWebhookSignatureException {
        try {
            WebhookSignatureValidator.validate(
                    xSignature,
                    xRequestId,
                    dataId,
                    mercadopagoProperties.getWebhookSecret(),
                    SIGNATURE_TOLERANCE);
        } catch (MPInvalidWebhookSignatureException ex) {
            log.warn(LOG_SIGNATURE_INVALID, ex.getReason(), ex.getRequestId());
            throw ex;
        }
    }
}