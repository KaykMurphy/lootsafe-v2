package com.lootsafe.payment.service;

import com.lootsafe.entity.Announcement;
import com.lootsafe.entity.Payment;
import com.lootsafe.entity.Transaction;
import com.lootsafe.enums.PaymentStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.repository.AnnouncementRepository;
import com.lootsafe.repository.PaymentRepository;
import com.lootsafe.repository.TransactionRepository;
import com.mercadopago.resources.order.Order;
import com.mercadopago.resources.order.OrderPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookReconciliationService {

    private static final String MSG_ORDER_REFERENCE_MISMATCH =
            "Referência externa da ordem não corresponde à cobrança local.";
    private static final String MSG_ORDER_AMOUNT_MISMATCH =
            "Valor da ordem não corresponde à cobrança local.";
    private static final String LOG_ORDER_UNKNOWN_LOCALLY =
            "Webhook ignorado: nenhuma cobrança local encontrada para a ordem {} (ref {}).";

    private final MercadoPagoClient mercadoPagoClient;
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final AnnouncementRepository announcementRepository;

    @Transactional
    public void reconcileOrder(String orderId) {
        Order order = mercadoPagoClient.getOrder(orderId);

        Payment payment = findLocalPayment(order);
        if (payment == null) {
            log.warn(LOG_ORDER_UNKNOWN_LOCALLY, order.getId(), order.getExternalReference());
            return;
        }

        validateOrderAgainstPayment(payment, order);
        updateEscrowFromOrder(payment, order);
    }

    private Payment findLocalPayment(Order order) {
        return paymentRepository.findByExternalId(order.getId())
                .orElseGet(() -> isBlank(order.getExternalReference())
                        ? null
                        : paymentRepository.findByExternalReference(order.getExternalReference()).orElse(null));
    }

    private void validateOrderAgainstPayment(Payment payment, Order order) {
        if (payment.getExternalReference() != null
                && !payment.getExternalReference().equals(order.getExternalReference())) {
            throw new BusinessException(MSG_ORDER_REFERENCE_MISMATCH);
        }

        if (order.getTotalAmount() == null
                || payment.getAmount().compareTo(new BigDecimal(order.getTotalAmount())) != 0) {
            throw new BusinessException(MSG_ORDER_AMOUNT_MISMATCH);
        }
    }

    private void updateEscrowFromOrder(Payment payment, Order order) {
        OrderPayment mercadoPagoPayment = findFirstPayment(order);
        String gatewayStatus = mercadoPagoPayment != null ? mercadoPagoPayment.getStatus() : order.getStatus();

        PaymentStatus newStatus = mapStatus(gatewayStatus);

        applyGatewayStatus(payment, newStatus, mercadoPagoPayment);

        if (newStatus == PaymentStatus.APPROVED && payment.getPaidAt() == null) {
            confirmApprovedPayment(payment);
        }
    }

    private void applyGatewayStatus(Payment payment,
                                    PaymentStatus newStatus,
                                    OrderPayment mercadoPagoPayment) {
        payment.setStatus(newStatus);
        if (mercadoPagoPayment != null && mercadoPagoPayment.getStatusDetail() != null) {
            payment.setStatusDetail(mercadoPagoPayment.getStatusDetail());
        }
        paymentRepository.save(payment);
    }

    private void confirmApprovedPayment(Payment payment) {
        payment.setPaidAt(Instant.now());

        Transaction transaction = payment.getTransaction();
        transaction.approve();
        transactionRepository.save(transaction);

        Announcement announcement = transaction.getAnnouncement();
        announcement.markAsSold();
        announcementRepository.save(announcement);
    }

    private PaymentStatus mapStatus(String gatewayStatus) {
        if (isBlank(gatewayStatus)) {
            return PaymentStatus.PENDING;
        }

        return switch (gatewayStatus.toLowerCase()) {
            case "approved", "processed", "accredited" -> PaymentStatus.APPROVED;
            case "pending", "in_process", "open", "action_required" -> PaymentStatus.PENDING;
            case "rejected" -> PaymentStatus.REJECTED;
            case "cancelled", "canceled" -> PaymentStatus.CANCELLED;
            case "refunded" -> PaymentStatus.REFUNDED;
            case "expired" -> PaymentStatus.EXPIRED;
            default -> PaymentStatus.PENDING;
        };
    }

    private OrderPayment findFirstPayment(Order order) {
        if (order.getTransactions() == null || order.getTransactions().getPayments() == null) {
            return null;
        }
        return order.getTransactions().getPayments().stream()
                .findFirst()
                .orElse(null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}