package com.lootsafe.service;

import com.lootsafe.entity.Payment;
import com.lootsafe.enums.PaymentStatus;
import com.lootsafe.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentExpirationService {

    private final PaymentRepository paymentRepository;
    private final TransactionService transactionService;

    public void expirePendingPayments() {

        List<Payment> expiredPayments = paymentRepository.findByStatusAndExpiresAtBefore(
                PaymentStatus.PENDING, Instant.now()
        );

        for (Payment payment : expiredPayments) {
            try {
                transactionService.cancelTransaction(payment.getTransaction().getId());
            } catch (Exception ex) {
                log.error("Falha ao processar expiração do pagamento {}", payment.getId(), ex);
            }
        }
    }
}