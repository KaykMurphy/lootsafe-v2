package com.lootsafe.payment.controller;

import com.lootsafe.payment.service.WebhookService;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class MercadoPagoWebhookController {

    private final WebhookService webhookService;

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> receiveMercadoPagoNotification(
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestHeader(name = "x-signature", required = false) String xSignature,
            @RequestHeader(name = "x-request-id", required = false) String xRequestId)
            throws MPInvalidWebhookSignatureException {

        webhookService.receiveNotification(dataId, xSignature, xRequestId);

        return ResponseEntity.ok().build();
    }
}