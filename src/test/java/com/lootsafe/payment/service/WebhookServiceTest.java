package com.lootsafe.payment.service;

import com.lootsafe.config.MercadoPagoProperties;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.exceptions.SignatureFailureReason;
import com.mercadopago.webhook.WebhookSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class WebhookServiceTest {

    private WebhookProcessorService webhookProcessorService;
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookProcessorService = mock(WebhookProcessorService.class);

        MercadoPagoProperties properties = new MercadoPagoProperties();
        properties.setWebhookSecret("test-webhook-secret");

        webhookService = new WebhookService(properties, webhookProcessorService);
    }

    @Test
    void validSignatureDelegatesToProcessing() throws MPInvalidWebhookSignatureException {
        try (MockedStatic<WebhookSignatureValidator> validator = mockStatic(WebhookSignatureValidator.class)) {
            validator.when(() -> WebhookSignatureValidator.validate(any(), any(), any(), any(), any()))
                    .thenAnswer(invocation -> null);

            webhookService.receiveNotification("ORDER123", "ts=1704908010,v1=any", "req-123");

            verify(webhookProcessorService).processOrder("ORDER123");
        }
    }

    @Test
    void invalidSignatureIsRejectedAndNotProcessed() {
        try (MockedStatic<WebhookSignatureValidator> validator = mockStatic(WebhookSignatureValidator.class)) {
            validator.when(() -> WebhookSignatureValidator.validate(any(), any(), any(), any(), any()))
                    .thenThrow(new MPInvalidWebhookSignatureException(
                            SignatureFailureReason.SIGNATURE_MISMATCH, "req-123", "1704908010"));

            assertThrows(MPInvalidWebhookSignatureException.class,
                    () -> webhookService.receiveNotification("ORDER123", "ts=1704908010,v1=invalid", "req-123"));

            verifyNoInteractions(webhookProcessorService);
        }
    }

    @Test
    void notificationWithoutOrderIdIsIgnored() throws MPInvalidWebhookSignatureException {
        try (MockedStatic<WebhookSignatureValidator> validator = mockStatic(WebhookSignatureValidator.class)) {
            validator.when(() -> WebhookSignatureValidator.validate(any(), any(), any(), any(), any()))
                    .thenAnswer(invocation -> null);

            webhookService.receiveNotification(null, "ts=1704908010,v1=any", "req-123");

            verifyNoInteractions(webhookProcessorService);
        }
    }
}