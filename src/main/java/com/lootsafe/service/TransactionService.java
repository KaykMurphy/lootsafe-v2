package com.lootsafe.service;

import com.lootsafe.dto.response.AnnouncementResponseDTO;
import com.lootsafe.dto.response.TransactionResponseDTO;
import com.lootsafe.entity.Announcement;
import com.lootsafe.entity.Transaction;
import com.lootsafe.entity.User;
import com.lootsafe.enums.AnnouncementStatus;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.mapper.TransactionMapper;
import com.lootsafe.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AnnouncementService announcementService;
    private final UserService userService;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponseDTO initiateTransaction(String announcementToken,
                                                      UUID buyerId) {

        Announcement announcement = announcementService.findEntityByToken(announcementToken);

        if (!announcement.getStatus().equals(AnnouncementStatus.ACTIVE)) {
            throw new BusinessException("Este anúncio não está disponível para compra.");
        }

        if (announcement.getSeller().getId().equals(buyerId)) {
            throw new BusinessException("Você não pode comprar seu próprio anúncio.");
        }

        User buyer = userService.findEntityById(buyerId);

        Transaction transaction = new Transaction();
        transaction.setAnnouncement(announcement);
        transaction.setBuyer(buyer);
        transaction.setSeller(announcement.getSeller());
        transaction.setAmount(announcement.getPrice());
        transaction.setStatus(TransactionStatus.PENDING);

        announcement.setStatus(AnnouncementStatus.SOLD);

        Transaction savedTransaction = transactionRepository.save(transaction);

        return transactionMapper.toResponse(savedTransaction);
    }


    public TransactionResponseDTO getTransactionById(UUID id) {
        Transaction transaction = findEntityById(id);
        return transactionMapper.toResponse(transaction);
    }

    public Transaction findEntityById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada."));
    }


    @Transactional
    public void updateTransactionStatus(UUID id, TransactionStatus newStatus) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada."));

        transaction.setStatus(newStatus);
    }


}
