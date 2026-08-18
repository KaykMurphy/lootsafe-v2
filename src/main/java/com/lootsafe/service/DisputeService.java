package com.lootsafe.service;

import com.lootsafe.dto.response.DisputeResponseDTO;
import com.lootsafe.entity.DisputeChat;
import com.lootsafe.entity.Payment;
import com.lootsafe.entity.Transaction;
import com.lootsafe.entity.User;
import com.lootsafe.enums.DisputeStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.exception.UnauthorizedException;
import com.lootsafe.mapper.DisputeMapper;
import com.lootsafe.payment.service.PaymentService;
import com.lootsafe.repository.DisputeRepository;
import com.lootsafe.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class DisputeService {

    private static final String MSG_DISPUTE_ALREADY_OPEN =
            "Já existe uma disputa aberta para esta transação.";
    private static final String MSG_NOT_TRANSACTION_PARTICIPANT =
            "Apenas o comprador ou o vendedor desta transação podem realizar esta ação.";
    private static final String MSG_DISPUTE_NOT_FOUND = "Disputa não encontrada.";
    private static final String MSG_DISPUTE_NOT_OPEN =
            "Apenas disputas em aberto podem ser resolvidas.";
    private static final String MSG_INVALID_RESOLUTION_STATUS =
            "Status de resolução inválido. Escolha RESOLVED_RELEASE ou RESOLVED_REFUND.";
    private static final String MSG_PAYMENT_NOT_FOUND =
            "Pagamento não encontrado.";

    private final DisputeRepository disputeRepository;
    private final TransactionService transactionService;
    private final DisputeMapper disputeMapper;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @Transactional
    public DisputeResponseDTO openDispute(UUID transactionId, UUID initiatedById, String reason) {

        Transaction transaction = transactionService.findEntityById(transactionId);

        if (disputeRepository.existsDisputeChatByTransaction(transaction)) {
            throw new BusinessException(MSG_DISPUTE_ALREADY_OPEN);
        }

        if (!transaction.getBuyer().getId().equals(initiatedById)
                && !transaction.getSeller().getId().equals(initiatedById)) {
            throw new UnauthorizedException(MSG_NOT_TRANSACTION_PARTICIPANT);
        }

        User initiatedBy = transaction.getBuyer().getId().equals(initiatedById)
                ? transaction.getBuyer()
                : transaction.getSeller();

        DisputeChat disputeChat = new DisputeChat();
        disputeChat.setTransaction(transaction);
        disputeChat.setInitiatedBy(initiatedBy);
        disputeChat.setReason(reason);
        disputeChat.setStatus(DisputeStatus.OPEN);

        transaction.markAsDisputed();

        DisputeChat savedDisputeChat = disputeRepository.save(disputeChat);

        return disputeMapper.toResponse(savedDisputeChat);
    }

    @Transactional
    public DisputeResponseDTO resolveDispute(UUID disputeId, DisputeStatus resolutionStatus, String resolutionNotes) {

        DisputeChat disputeChat = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DISPUTE_NOT_FOUND));

        if (disputeChat.getStatus() != DisputeStatus.OPEN) {
            throw new BusinessException(MSG_DISPUTE_NOT_OPEN);
        }

        Transaction transaction = disputeChat.getTransaction();

        switch (resolutionStatus) {
            case RESOLVED_RELEASE -> transaction.release();

            case RESOLVED_REFUND -> {
                transaction.refund();

                Payment latestPayment = paymentRepository.findByTransactionId(transaction.getId()).stream()
                        .max(Comparator.comparing(payment -> payment.getCreatedAt()))
                        .orElseThrow(() -> new ResourceNotFoundException(MSG_PAYMENT_NOT_FOUND));

                paymentService.refundPayment(latestPayment.getId());
            }

            default -> throw new BusinessException(MSG_INVALID_RESOLUTION_STATUS);
        }

        disputeChat.setStatus(resolutionStatus);
        disputeChat.setResolutionNotes(resolutionNotes);

        DisputeChat savedDisputeChat = disputeRepository.save(disputeChat);

        return disputeMapper.toResponse(savedDisputeChat);
    }

    public List<DisputeResponseDTO> listDisputes() {
        return disputeRepository.findAll()
                .stream()
                .map(disputeMapper::toResponse)
                .toList();
    }
}