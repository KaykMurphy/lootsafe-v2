package com.lootsafe.scheduler;

import com.lootsafe.service.PaymentExpirationService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PaymentScheduler {

    private final PaymentExpirationService paymentExpirationService;

    @Scheduled(fixedDelayString = "${payment.expiration-check-interval-ms:3600000}")
    @SchedulerLock(name = "expirePendingPaymentsLock", lockAtLeastFor = "15s", lockAtMostFor = "5m")
    public void checkAndExpirePayments() {
        paymentExpirationService.expirePendingPayments();
    }
}