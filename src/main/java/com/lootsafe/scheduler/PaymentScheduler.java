package com.lootsafe.scheduler;

import com.lootsafe.service.PaymentExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PaymentScheduler {

    private final PaymentExpirationService paymentExpirationService;

    @Scheduled(fixedDelayString = "${payment.expiration-check-interval-ms}")
    public void checkAndExpirePayments() {
        paymentExpirationService.expirePendingPayments();
    }
}