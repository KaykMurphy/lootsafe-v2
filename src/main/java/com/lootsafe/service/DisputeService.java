package com.lootsafe.service;

import com.lootsafe.entity.DisputeChat;
import com.lootsafe.entity.Transaction;
import com.lootsafe.entity.User;
import com.lootsafe.enums.DisputeStatus;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.exception.UnauthorizedException;
import com.lootsafe.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final TransactionService transactionService;
    private final UserService userService;

    @Transactional
    public DisputeChat openDispute(UUID transactionId, UUID initiatedById,
                                   String reason){

        Transaction transaction = transactionService.getTransactionById(transactionId);

        if (disputeRepository.existsDisputeChatByTransaction(transaction)){
            throw new BusinessException("Já existe uma disputa aberta para esta transação.");
        }

        if (!transaction.getBuyer().getId().equals(initiatedById)
                && !transaction.getSeller().getId().equals(initiatedById)) {

            throw new UnauthorizedException("Apenas o comprador ou o vendedor desta transação podem realizar esta ação.");
        }

        User initiatedBy = userService.findById(initiatedById);

        DisputeChat disputeChat = new DisputeChat();
        disputeChat.setTransaction(transaction);
        disputeChat.setInitiatedBy(initiatedBy);
        disputeChat.setReason(reason);
        disputeChat.setStatus(DisputeStatus.OPEN);

        transaction.setStatus(TransactionStatus.DISPUTED);

        return disputeRepository.save(disputeChat);
    }

    @Transactional
    public DisputeChat resolveDispute(UUID disputeId, DisputeStatus resolutionStatus,
                                      String resolutionNotes) {

        DisputeChat disputeChat = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Disputa não encontrada."));

        if (!disputeChat.getStatus().equals(DisputeStatus.OPEN)) {
            throw new BusinessException("Apenas disputas em aberto podem ser resolvidas.");
        }

        disputeChat.setStatus(resolutionStatus);
        disputeChat.setResolutionNotes(resolutionNotes);

        Transaction transaction = disputeChat.getTransaction();

        if (resolutionStatus.equals(DisputeStatus.RESOLVED_RELEASE)) {
            transaction.setStatus(TransactionStatus.RELEASED);
        } else if (resolutionStatus.equals(DisputeStatus.RESOLVED_REFUND)) {
            transaction.setStatus(TransactionStatus.REFUNDED);
        }

        return disputeChat;
    }



}
