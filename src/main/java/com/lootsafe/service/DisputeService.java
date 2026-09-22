package com.lootsafe.service;

import com.lootsafe.dto.response.DisputeResponseDTO;
import com.lootsafe.entity.DisputeChat;
import com.lootsafe.entity.Payment;
import com.lootsafe.entity.Transaction;
import com.lootsafe.entity.User;
import com.lootsafe.enums.DisputeStatus;
import com.lootsafe.enums.PaymentStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.exception.UnauthorizedException;
import com.lootsafe.mapper.DisputeMapper;
import com.lootsafe.payment.payout.PayoutService;
import com.lootsafe.payment.service.PaymentService;
import com.lootsafe.repository.DisputeRepository;
import com.lootsafe.repository.PaymentRepository;
import com.lootsafe.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
    private static final String MSG_APPROVED_PAYMENT_NOT_FOUND =
            "Pagamento aprovado não encontrado para esta transação.";

    private static final String MSG_NOT_DISPUTE_INITIATOR =
             "Apenas o usuário que iniciou a disputa pode cancelá-la.";

    private final DisputeRepository disputeRepository;
    private final TransactionService transactionService;
    private final DisputeMapper disputeMapper;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final PayoutService payoutService;
    private final TransactionRepository transactionRepository;

    @Transactional
    public DisputeResponseDTO openDispute(UUID transactionId, UUID initiatedById, String reason) {

        Transaction transaction = transactionService.findEntityById(transactionId);

        if (!transaction.getBuyer().getId().equals(initiatedById)
                && !transaction.getSeller().getId().equals(initiatedById)) {
            throw new UnauthorizedException(MSG_NOT_TRANSACTION_PARTICIPANT);
        }

        User initiatedBy = transaction.getBuyer().getId().equals(initiatedById)
                ? transaction.getBuyer()
                : transaction.getSeller();

        Optional<DisputeChat> existingDisputeOpt = disputeRepository.findByTransactionId(transactionId);

        DisputeChat disputeChat;

        if (existingDisputeOpt.isPresent()) {
            disputeChat = existingDisputeOpt.get();

            if (disputeChat.getStatus() == DisputeStatus.OPEN) {
                throw new BusinessException(MSG_DISPUTE_ALREADY_OPEN);
            }

            disputeChat.setReason(reason);
            disputeChat.setInitiatedBy(initiatedBy);
            disputeChat.setStatus(DisputeStatus.OPEN);
        } else {
            disputeChat = new DisputeChat();
            disputeChat.setTransaction(transaction);
            disputeChat.setInitiatedBy(initiatedBy);
            disputeChat.setReason(reason);
            disputeChat.setStatus(DisputeStatus.OPEN);
        }

        transaction.markAsDisputed();
        transactionRepository.save(transaction);

        DisputeChat savedDisputeChat = disputeRepository.save(disputeChat);

        return disputeMapper.toResponse(savedDisputeChat);
    }

    @Transactional
    public DisputeResponseDTO cancelDispute(UUID disputeId, UUID currentUserId){

        DisputeChat disputeChat = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DISPUTE_NOT_FOUND));

        if (!disputeChat.getStatus().equals(DisputeStatus.OPEN)){
            throw new BusinessException(MSG_DISPUTE_NOT_OPEN);
        }

        if (!disputeChat.getInitiatedBy().getId().equals(currentUserId)) {
             throw new UnauthorizedException(MSG_NOT_DISPUTE_INITIATOR);
        }

        Transaction transaction = disputeChat.getTransaction();

        transaction.cancelDispute();

        transactionRepository.save(transaction);

        disputeChat.setStatus(DisputeStatus.CANCELLED);

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
            case RESOLVED_RELEASE -> {
                transaction.release();

                transactionRepository.save(transaction);
                payoutService.processPayout(transaction.getId());
            }

            case RESOLVED_REFUND -> {
                transaction.refund();

                Payment approvedPayment = paymentRepository
                        .findByTransactionIdAndStatus(
                                transaction.getId(),
                                PaymentStatus.APPROVED
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(MSG_APPROVED_PAYMENT_NOT_FOUND)
                        );

                paymentService.refundPayment(approvedPayment.getId());
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

    public DisputeResponseDTO findByTransactionId(UUID transactionId) {
        DisputeChat disputeChat = disputeRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma disputa encontrada para esta transação."));
        return disputeMapper.toResponse(disputeChat);
    }
}