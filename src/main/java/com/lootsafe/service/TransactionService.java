package com.lootsafe.service;

import com.lootsafe.dto.response.PaymentResponseDTO;
import com.lootsafe.dto.response.TransactionResponseDTO;
import com.lootsafe.entity.Announcement;
import com.lootsafe.entity.Payment;
import com.lootsafe.entity.Transaction;
import com.lootsafe.entity.User;
import com.lootsafe.enums.AnnouncementStatus;
import com.lootsafe.enums.PaymentStatus;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.exception.UnauthorizedException;
import com.lootsafe.mapper.PaymentMapper;
import com.lootsafe.mapper.TransactionMapper;
import com.lootsafe.payment.service.PaymentService;
import com.lootsafe.repository.AnnouncementRepository;
import com.lootsafe.repository.PaymentRepository;
import com.lootsafe.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class TransactionService {

    private static final String MSG_CANNOT_BUY_OWN_ANNOUNCEMENT =
            "Você não pode comprar seu próprio anúncio.";
    private static final String MSG_TRANSACTION_NOT_FOUND = "Transação não encontrada.";

    private static final String MGG_UNATHORIZED_USER = "Você não é o comprador desta transação";

    private static final String MSG_TRANSACTION_CANNOT_BE_REFUNDED =
            "A transação só pode ser reembolsada quando estiver aprovada ou em disputa.";

    private static final String MSG_APPROVED_PAYMENT_NOT_FOUND_FOR_REFUND =
            "Pagamento aprovado não encontrado para reembolso.";

    private static final String MSG_TRANSACTION_CANNOT_BE_CANCELLED =
            "A transação só pode ser cancelada quando estiver pendente.";

    private static final String MSG_ANNOUNCEMENT_NOT_FOUND =
            "Anúncio não encontrado ou link inválido.";


    private final TransactionRepository transactionRepository;
    private final AnnouncementRepository announcementRepository;
    private final UserService userService;
    private final TransactionMapper transactionMapper;
    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;

    @Transactional
    public TransactionResponseDTO initiateTransaction(String announcementToken,
                                                      UUID buyerId) {

        Announcement announcement = announcementRepository.findByToken(announcementToken)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_ANNOUNCEMENT_NOT_FOUND));

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

        PaymentResponseDTO paymentDTO = paymentService.createPayment(savedTransaction.getId());

        return buildResponse(savedTransaction, paymentDTO);
    }

    @Transactional
    public TransactionResponseDTO refundTransaction(UUID transactionId) {
        Transaction transaction = findEntityById(transactionId);

        if (transaction.getStatus() != TransactionStatus.APPROVED
                && transaction.getStatus() != TransactionStatus.DISPUTED) {
            throw new BusinessException(MSG_TRANSACTION_CANNOT_BE_REFUNDED);
        }

        Payment approvedPayment = paymentRepository.findByTransactionIdAndStatus(transactionId, PaymentStatus.APPROVED)
                .orElseThrow(() -> new BusinessException(MSG_APPROVED_PAYMENT_NOT_FOUND_FOR_REFUND));

        paymentService.refundPayment(approvedPayment.getId());

        transaction.setStatus(TransactionStatus.REFUNDED);
        Transaction savedTransaction = transactionRepository.save(transaction);

        PaymentResponseDTO paymentDTO = paymentMapper.toResponse(approvedPayment);

        return buildResponse(savedTransaction, paymentDTO);
    }

    @Transactional
    public TransactionResponseDTO cancelTransaction(UUID transactionId) {
        Transaction transaction = findEntityById(transactionId);

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException(MSG_TRANSACTION_CANNOT_BE_CANCELLED);
        }

        paymentRepository.findByTransactionId(transactionId).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .forEach(payment -> paymentService.cancelPayment(payment.getId()));

        transaction.setStatus(TransactionStatus.CANCELLED);
        Transaction savedTransaction = transactionRepository.save(transaction);

        Announcement announcement = savedTransaction.getAnnouncement();
        if (announcement != null && announcement.getStatus() == AnnouncementStatus.RESERVED) {
            announcement.setStatus(AnnouncementStatus.ACTIVE);
        }

        Payment payment = paymentService.findLatestPayment(savedTransaction.getId());
        PaymentResponseDTO paymentDTO = payment == null ? null : paymentMapper.toResponse(payment);

        return buildResponse(savedTransaction, paymentDTO);
    }

    public List<TransactionResponseDTO> listTransactions(TransactionStatus status) {
        List<Transaction> transactions;

        if (status == null) {
            transactions = transactionRepository.findAll();
        } else {
            transactions = transactionRepository.findByStatus(status);
        }

        return transactions.stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    public TransactionResponseDTO getTransactionById(UUID id) {
        Transaction transaction = findEntityById(id);
        Payment payment = paymentService.findLatestPayment(transaction.getId());

        PaymentResponseDTO paymentDTO = payment == null ? null : paymentMapper.toResponse(payment);

        return buildResponse(transaction, paymentDTO);
    }

    public Transaction findEntityById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSACTION_NOT_FOUND));
    }

    @Transactional
    public TransactionResponseDTO confirmReceipt(UUID transactionId, UUID buyerId) {
        Transaction transaction = findEntityById(transactionId);

        if (!transaction.getBuyer().getId().equals(buyerId)) {
            throw new UnauthorizedException(MGG_UNATHORIZED_USER);
        }

        transaction.confirmReceipt();

        Transaction savedTransaction = transactionRepository.save(transaction);

        Payment payment = paymentService.findLatestPayment(savedTransaction.getId());
        PaymentResponseDTO paymentDTO = payment == null ? null : paymentMapper.toResponse(payment);

        return buildResponse(savedTransaction, paymentDTO);
    }

    private TransactionResponseDTO buildResponse(Transaction transaction, PaymentResponseDTO paymentResponse) {
        TransactionResponseDTO base = transactionMapper.toResponse(transaction);

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