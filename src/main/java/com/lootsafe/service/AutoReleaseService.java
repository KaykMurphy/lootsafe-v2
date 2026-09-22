package com.lootsafe.service;

import com.lootsafe.entity.Transaction;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.payment.payout.PayoutService;
import com.lootsafe.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoReleaseService {

    private final TransactionRepository transactionRepository;
    private final PayoutService payoutService;

    public void processAutoReleases() {
        Instant now = Instant.now();

        List<Transaction> expiredTransactions = transactionRepository
                .findByStatusAndInspectionExpiresAtBefore(TransactionStatus.APPROVED, now);

        if (expiredTransactions.isEmpty()) {
            log.debug("Nenhuma transação com prazo de inspeção expirado encontrada para auto-release.");
            return;
        }

        log.info("Encontradas {} transações prontas para liberação automática.", expiredTransactions.size());

        for (Transaction transaction : expiredTransactions) {
            try {
                releaseAndPayout(transaction);
            } catch (Exception ex) {
                log.error("Erro inesperado ao processar auto-release da transacao={}: {}",
                        transaction.getId(), ex.getMessage(), ex);
            }
        }
    }

    @Transactional
    public void releaseAndPayout(Transaction transaction) {
        log.info("Executando auto-release para a transacao={}, expirada em={}.",
                transaction.getId(), transaction.getInspectionExpiresAt());

        transaction.autoRelease();

        Transaction saved = transactionRepository.save(transaction);

        payoutService.processPayout(saved.getId());
    }
}