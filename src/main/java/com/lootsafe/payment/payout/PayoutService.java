package com.lootsafe.payment.payout;

import com.lootsafe.entity.Transaction;
import com.lootsafe.enums.PayoutStatus;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutService {

    private static final String MSG_TRANSACTION_NOT_FOUND = "Transação não encontrada.";
    private static final String MSG_TRANSACTION_NOT_RELEASED = "Apenas transações com status RELEASED podem receber Payout.";
    private static final String MSG_PAYOUT_ALREADY_COMPLETED = "O Payout desta transação já foi concluído.";
    private static final String MSG_SELLER_PIX_KEY_MISSING = "Chave Pix do vendedor não cadastrada no anúncio.";

    private final TransactionRepository transactionRepository;
    private final PayoutClient payoutClient;

    @Transactional
    public void processPayout(UUID transactionId) {
        if (transactionId == null) {
            log.warn("Tentativa de processar payout com transactionId nulo.");
            return;
        }

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSACTION_NOT_FOUND));

        if (!isEligibleForPayout(transaction)) {
            log.warn("Transação id={} não está elegível para payout (status={}, payoutStatus={}).",
                    transactionId, transaction.getStatus(), transaction.getPayoutStatus());
            return;
        }

        String pixKey = transaction.getAnnouncement() != null ? transaction.getAnnouncement().getPixKey() : null;
        if (pixKey == null || pixKey.isBlank()) {
            log.error("Falha ao processar Payout Pix da transacao={}. Erro: {}. Marcado para retry.",
                    transactionId, MSG_SELLER_PIX_KEY_MISSING);
            transaction.setPayoutStatus(PayoutStatus.FAILED);
            transaction.setPayoutFailureReason(MSG_SELLER_PIX_KEY_MISSING);
            transactionRepository.save(transaction);
            return;
        }

        BigDecimal transferAmount = transaction.getNetAmount() != null
                ? transaction.getNetAmount()
                : transaction.getAmount();

        String idempotencyKey = "payout-" + transactionId;
        String description = "Repasse LootSafe - Transacao " + transactionId;

        log.info("Iniciando transferencia Pix da transacao={} para chavePix={} no valor={}.",
                transactionId, pixKey, transferAmount);

        try {
            PayoutResult result = payoutClient.transferPix(pixKey, transferAmount, idempotencyKey, description);

            if (result != null && (result.status() == PayoutStatus.PAID || result.status() == PayoutStatus.COMPLETED)) {
                transaction.setPayoutStatus(PayoutStatus.PAID);
                transaction.setPayoutExternalId(result.externalTransferId());
                transaction.setPayoutPaidAt(result.processedAt() != null ? result.processedAt() : Instant.now());
                transaction.setPayoutFailureReason(null);

                log.info("Payout Pix concluido com sucesso. Transacao={}, ExternalId={}, Valor={}.",
                        transactionId, result.externalTransferId(), transferAmount);
            } else if (result != null && result.status() == PayoutStatus.PROCESSING) {
                transaction.setPayoutStatus(PayoutStatus.PROCESSING);
                transaction.setPayoutExternalId(result.externalTransferId());
                transaction.setPayoutFailureReason(null);

                log.info("Payout Pix em processamento para a transacao={}, ExternalId={}.",
                        transactionId, result.externalTransferId());
            } else {
                String errorMessage = result != null && result.errorMessage() != null
                        ? result.errorMessage()
                        : "Falha desconhecida no provedor de Payout.";

                transaction.setPayoutStatus(PayoutStatus.FAILED);
                transaction.setPayoutFailureReason(errorMessage);

                log.error("Falha ao processar Payout Pix da transacao={}. Erro: {}. Marcado para retry.",
                        transactionId, errorMessage);
            }
        } catch (Exception e) {
            String error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            transaction.setPayoutStatus(PayoutStatus.FAILED);
            transaction.setPayoutFailureReason(error);

            log.error("Falha ao processar Payout Pix da transacao={}. Erro: {}. Marcado para retry.",
                    transactionId, error);
        }

        transactionRepository.save(transaction);
    }

    public boolean isEligibleForPayout(Transaction transaction) {
        if (transaction == null || transaction.getStatus() != TransactionStatus.RELEASED) {
            return false;
        }

        PayoutStatus status = transaction.getPayoutStatus();
        return status == null
                || status == PayoutStatus.PENDING
                || status == PayoutStatus.FAILED
                || status == PayoutStatus.NOT_APPLICABLE;
    }
}