package com.lootsafe.payment.service;

import com.mercadopago.client.order.OrderClient;
import com.mercadopago.client.order.OrderCreateRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.Headers;
import com.mercadopago.resources.order.Order;
import com.lootsafe.exception.PaymentProviderException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MercadoPagoClient {

    private final OrderClient orderClient;

    public MercadoPagoClient(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    public Order createOrder(OrderCreateRequest request, String idempotencyKey) {
        MPRequestOptions requestOptions = MPRequestOptions.builder()
                .customHeaders(Map.of(Headers.IDEMPOTENCY_KEY, idempotencyKey))
                .build();

        return invoke(() -> orderClient.create(request, requestOptions));
    }

    public Order getOrder(String orderId) {
        return invoke(() -> orderClient.get(orderId));
    }

    public Order cancelOrder(String orderId) {
        return invoke(() -> orderClient.cancel(orderId));
    }

    private <T> T invoke(MercadoPagoOperation<T> operation) {
        try {
            return operation.execute();
        } catch (MPApiException ex) {
            throw new PaymentProviderException(
                    "O Mercado Pago retornou um erro (HTTP " + ex.getStatusCode() + ").", ex);
        } catch (MPException ex) {
            throw new PaymentProviderException("Falha de comunicação com o Mercado Pago.", ex);
        }
    }

    @FunctionalInterface
    private interface MercadoPagoOperation<T> {
        T execute() throws MPException, MPApiException;
    }
}