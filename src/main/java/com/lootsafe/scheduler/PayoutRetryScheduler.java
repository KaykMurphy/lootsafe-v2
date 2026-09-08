package com.lootsafe.scheduler;

import com.lootsafe.entity.Transaction;
import com.lootsafe.enums.PayoutStatus;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.payment.payout.PayoutService;
import com.lootsafe.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayoutRetryScheduler {

    private final TransactionRepository transactionRepository;
    private final PayoutService payoutService;

    @Scheduled(fixedDelayString = "${payout.retry-interval-ms:300000}")
    @SchedulerLock(name = "retryFailedPayoutsLock", lockAtLeastFor = "30s", lockAtMostFor = "10m")
    public void retryPayouts() {
        List<Transaction> pendingPayouts = transactionRepository.findByStatusAndPayoutStatusIn(
                TransactionStatus.RELEASED,
                List.of(PayoutStatus.PENDING, PayoutStatus.FAILED)
        );

        if (pendingPayouts.isEmpty()) {
            return;
        }

        log.info("Encontradas {} transações com repasse Pix pendente/falho para retentativa.", pendingPayouts.size());

        for (Transaction transaction : pendingPayouts) {
            try {
                log.info("Retentando repasse Pix da transacao={}", transaction.getId());
                payoutService.processPayout(transaction.getId());
            } catch (Exception ex) {
                log.error("Erro ao retentar repasse da transacao={}: {}", transaction.getId(), ex.getMessage());
            }
        }
    }
}