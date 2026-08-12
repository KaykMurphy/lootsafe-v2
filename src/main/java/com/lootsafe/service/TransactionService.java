package com.lootsafe.service;

import com.lootsafe.dto.response.PaymentResponseDTO;
import com.lootsafe.dto.response.TransactionResponseDTO;
import com.lootsafe.entity.Announcement;
import com.lootsafe.entity.Payment;
import com.lootsafe.entity.Transaction;
import com.lootsafe.entity.User;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.mapper.PaymentMapper;
import com.lootsafe.mapper.TransactionMapper;
import com.lootsafe.payment.service.PaymentService;
import com.lootsafe.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TransactionService {

    private static final String MSG_CANNOT_BUY_OWN_ANNOUNCEMENT =
            "Você não pode comprar seu próprio anúncio.";
    private static final String MSG_TRANSACTION_NOT_FOUND = "Transação não encontrada.";

    private final TransactionRepository transactionRepository;
    private final AnnouncementService announcementService;
    private final UserService userService;
    private final TransactionMapper transactionMapper;
    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    @Transactional
    public TransactionResponseDTO initiateTransaction(String announcementToken,
                                                      UUID buyerId) {

        Announcement announcement = announcementService.findEntityByToken(announcementToken);

        if (announcement.getSeller().getId().equals(buyerId)) {
            throw new BusinessException(MSG_CANNOT_BUY_OWN_ANNOUNCEMENT);
        }

        User buyer = userService.findEntityById(buyerId);

        Transaction transaction = new Transaction();
        transaction.setAnnouncement(announcement);
        transaction.setBuyer(buyer);
        transaction.setSeller(announcement.getSeller());
        transaction.setAmount(announcement.getPrice());
        transaction.setStatus(TransactionStatus.PENDING);

        announcement.reserve();

        Transaction savedTransaction = transactionRepository.save(transaction);

        Payment payment = paymentService.createPayment(savedTransaction.getId());

        return buildResponse(savedTransaction, payment);
    }


    public TransactionResponseDTO getTransactionById(UUID id) {
        Transaction transaction = findEntityById(id);
        Payment payment = paymentService.findLatestPayment(transaction.getId());
        return buildResponse(transaction, payment);
    }

    public Transaction findEntityById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSACTION_NOT_FOUND));
    }

    private TransactionResponseDTO buildResponse(Transaction transaction, Payment payment) {
        TransactionResponseDTO base = transactionMapper.toResponse(transaction);
        PaymentResponseDTO paymentResponse = payment == null ? null : paymentMapper.toResponse(payment);

        return new TransactionResponseDTO(
                base.id(),
                base.announcementId(),
                base.buyerId(),
                base.sellerId(),
                base.status(),
                base.amount(),
                base.createdAt(),
                base.updatedAt(),
                paymentResponse
        );
    }
}