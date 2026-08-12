package com.lootsafe.payment.service;

import com.lootsafe.entity.Payment;
import com.lootsafe.entity.Transaction;
import com.lootsafe.enums.PaymentProvider;
import com.lootsafe.enums.PaymentStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.repository.PaymentRepository;
import com.lootsafe.repository.TransactionRepository;
import com.mercadopago.client.order.OrderCreateRequest;
import com.mercadopago.client.order.OrderPayerRequest;
import com.mercadopago.client.order.OrderPaymentMethodRequest;
import com.mercadopago.client.order.OrderPaymentRequest;
import com.mercadopago.client.order.OrderTransactionRequest;
import com.mercadopago.resources.order.Order;
import com.mercadopago.resources.order.OrderPayment;
import com.mercadopago.resources.order.OrderPaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int PIX_VALIDITY_HOURS = 24;

    private static final String MSG_TRANSACTION_NOT_FOUND = "Transação não encontrada.";
    private static final String MSG_TRANSACTION_NOT_PENDING =
            "Esta transação não está em estado pendente.";
    private static final String MSG_ACTIVE_PAYMENT_ALREADY_EXISTS =
            "Já existe um pagamento pendente para esta transação.";

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final MercadoPagoClient mercadoPagoClient;

    @Transactional
    public Payment createPayment(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSACTION_NOT_FOUND));

        validateTransactionCanReceivePayment(transaction);

        Instant expiresIn = Instant.now().plus(PIX_VALIDITY_HOURS, ChronoUnit.HOURS);
        String idempotencyKey = UUID.randomUUID().toString();
        String externalReference = transactionId.toString();

        OrderCreateRequest request = buildOrderRequest(transaction, expiresIn, externalReference);

        Order order = mercadoPagoClient.createOrder(request, idempotencyKey);

        Payment payment = buildPayment(transaction, expiresIn, idempotencyKey, externalReference, order);

        return paymentRepository.save(payment);
    }

    public Payment findLatestPayment(UUID transactionId) {
        return paymentRepository.findByTransactionId(transactionId).stream()
                .max(Comparator.comparing(Payment::getCreatedAt))
                .orElse(null);
    }

    private void validateTransactionCanReceivePayment(Transaction transaction) {
        if (!transaction.isPending()) {
            throw new BusinessException(MSG_TRANSACTION_NOT_PENDING);
        }

        boolean hasActivePayment = paymentRepository.findByTransactionId(transaction.getId()).stream()
                .anyMatch(payment -> PaymentStatus.PENDING.equals(payment.getStatus())
                        && (payment.getExpiresAt() == null || payment.getExpiresAt().isAfter(Instant.now())));

        if (hasActivePayment) {
            throw new BusinessException(MSG_ACTIVE_PAYMENT_ALREADY_EXISTS);
        }
    }

    private OrderCreateRequest buildOrderRequest(Transaction transaction,
                                                 Instant expiresIn,
                                                 String externalReference) {
        return OrderCreateRequest.builder()
                .type("online")
                .processingMode("automatic")
                .totalAmount(transaction.getAmount().toPlainString())
                .externalReference(externalReference)
                .payer(OrderPayerRequest.builder()
                        .email(transaction.getBuyer().getEmail())
                        .build())
                .transactions(OrderTransactionRequest.builder()
                        .payments(List.of(OrderPaymentRequest.builder()
                                .amount(transaction.getAmount().toPlainString())
                                .paymentMethod(OrderPaymentMethodRequest.builder()
                                        .id("pix")
                                        .type("bank_transfer")
                                        .build())
                                .dateOfExpiration(expiresIn.atOffset(ZoneOffset.UTC).toString())
                                .build()))
                        .build())
                .build();
    }

    private Payment buildPayment(Transaction transaction,
                                 Instant expiresIn,
                                 String idempotencyKey,
                                 String externalReference,
                                 Order order) {
        Payment payment = new Payment();
        payment.setTransaction(transaction);
        payment.setProvider(PaymentProvider.MERCADO_PAGO);
        payment.setAmount(transaction.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setExternalReference(externalReference);
        payment.setExternalId(order.getId());

        OrderPayment mercadoPagoPayment = findFirstPayment(order);
        if (mercadoPagoPayment != null) {
            OrderPaymentMethod paymentMethod = mercadoPagoPayment.getPaymentMethod();
            if (paymentMethod != null) {
                payment.setPaymentMethod(paymentMethod.getId());
                payment.setPixCode(paymentMethod.getQrCode());
                payment.setQrCodeBase64(paymentMethod.getQrCodeBase64());
                payment.setTicketUrl(paymentMethod.getTicketUrl());
            }
            payment.setStatusDetail(mercadoPagoPayment.getStatusDetail());
            if (mercadoPagoPayment.getDateOfExpiration() != null) {
                payment.setExpiresAt(OffsetDateTime.parse(mercadoPagoPayment.getDateOfExpiration()).toInstant());
            }
        }

        if (payment.getExpiresAt() == null) {
            payment.setExpiresAt(expiresIn);
        }

        return payment;
    }

    private OrderPayment findFirstPayment(Order order) {
        if (order.getTransactions() == null || order.getTransactions().getPayments() == null) {
            return null;
        }
        return order.getTransactions().getPayments().stream()
                .findFirst()
                .orElse(null);
    }
}