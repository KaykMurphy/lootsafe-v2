package com.lootsafe.service;

import com.lootsafe.dto.response.DisputeResponseDTO;
import com.lootsafe.entity.DisputeChat;
import com.lootsafe.entity.Transaction;
import com.lootsafe.entity.User;
import com.lootsafe.enums.DisputeStatus;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.exception.UnauthorizedException;
import com.lootsafe.mapper.DisputeMapper;
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
    private final DisputeMapper disputeMapper;


    @Transactional
    public DisputeResponseDTO openDispute(UUID transactionId, UUID initiatedById, String reason) {

        Transaction transaction = transactionService.findEntityById(transactionId);

        if (disputeRepository.existsDisputeChatByTransaction(transaction)) {
            throw new BusinessException("Já existe uma disputa aberta para esta transação.");
        }

        if (!transaction.getBuyer().getId().equals(initiatedById)
                && !transaction.getSeller().getId().equals(initiatedById)) {
            throw new UnauthorizedException("Apenas o comprador ou o vendedor desta transação podem realizar esta ação.");
        }

        User initiatedBy = transaction.getBuyer().getId().equals(initiatedById)
                ? transaction.getBuyer()
                : transaction.getSeller();

        DisputeChat disputeChat = new DisputeChat();
        disputeChat.setTransaction(transaction);
        disputeChat.setInitiatedBy(initiatedBy);
        disputeChat.setReason(reason);
        disputeChat.setStatus(DisputeStatus.OPEN);

        transaction.setStatus(TransactionStatus.DISPUTED);

        DisputeChat savedDisputeChat = disputeRepository.save(disputeChat);

        return disputeMapper.toResponse(savedDisputeChat);
    }

    @Transactional
    public DisputeResponseDTO resolveDispute(UUID disputeId, DisputeStatus resolutionStatus, String resolutionNotes) {

        DisputeChat disputeChat = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Disputa não encontrada."));

        if (!disputeChat.getStatus().equals(DisputeStatus.OPEN)) {
            throw new BusinessException("Apenas disputas em aberto podem ser resolvidas.");
        }

        if (resolutionStatus == DisputeStatus.RESOLVED_RELEASE) {
            disputeChat.getTransaction().setStatus(TransactionStatus.RELEASED);
        } else if (resolutionStatus == DisputeStatus.RESOLVED_REFUND) {
            disputeChat.getTransaction().setStatus(TransactionStatus.REFUNDED);
        } else {
            throw new BusinessException("Status de resolução inválido. Escolha RESOLVED_RELEASE ou RESOLVED_REFUND.");
        }

        disputeChat.setStatus(resolutionStatus);
        disputeChat.setResolutionNotes(resolutionNotes);

        DisputeChat savedDisputeChat = disputeRepository.save(disputeChat);

        return disputeMapper.toResponse(savedDisputeChat);
    }



}
