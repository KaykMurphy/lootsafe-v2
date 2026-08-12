package com.lootsafe.payment.service;

import com.lootsafe.config.MercadoPagoProperties;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.webhook.WebhookSignatureValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class WebhookService {

    private static final Duration SIGNATURE_TOLERANCE = Duration.ofMinutes(5);

    private static final String LOG_WEBHOOK_WITHOUT_ORDER =
            "Webhook recebido sem identificador de ordem.";
    private static final String LOG_SIGNATURE_INVALID =
            "Assinatura do webhook rejeitada (reason={}, requestId={}).";

    private final MercadoPagoProperties mercadopagoProperties;
    private final WebhookProcessorService webhookProcessorService;

    public WebhookService(MercadoPagoProperties mercadopagoProperties,
                          WebhookProcessorService webhookProcessorService) {
        this.mercadopagoProperties = mercadopagoProperties;
        this.webhookProcessorService = webhookProcessorService;
    }

    public void receiveNotification(String dataId, String xSignature, String xRequestId)
            throws MPInvalidWebhookSignatureException {
        validateSignature(xSignature, xRequestId, dataId);

        if (dataId == null || dataId.isBlank()) {
            log.warn(LOG_WEBHOOK_WITHOUT_ORDER);
            return;
        }

        webhookProcessorService.processOrder(dataId);
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