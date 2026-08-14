package com.lootsafe.service;

import com.lootsafe.dto.response.CredentialsResponseDTO;
import com.lootsafe.entity.Transaction;
import com.lootsafe.enums.PaymentStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.UnauthorizedException;
import com.lootsafe.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DigitalProductDeliveryService {

    private static final String MSG_NOT_THE_BUYER = "Você não é o comprador desta transação.";
    private static final String MSG_CREDENTIALS_NOT_RELEASED =
            "As credenciais só são liberadas após a aprovação do pagamento.";
    private static final String MSG_PAYMENT_NOT_APPROVED = "Pagamento não aprovado.";

    private final TransactionService transactionService;
    private final PaymentRepository paymentRepository;
    private final EncryptionService encryptionService;

    public CredentialsResponseDTO deliverCredentials(UUID transactionId, UUID buyerId) {
        Transaction transaction = transactionService.findEntityById(transactionId);

        if (!transaction.getBuyer().getId().equals(buyerId)) {
            throw new UnauthorizedException(MSG_NOT_THE_BUYER);
        }

        if (!transaction.isApproved()) {
            throw new BusinessException(MSG_CREDENTIALS_NOT_RELEASED);
        }

        boolean paymentApproved = paymentRepository.findByTransactionId(transaction.getId()).stream()
                .anyMatch(payment -> payment.getStatus() == PaymentStatus.APPROVED);

        if (!paymentApproved) {
            throw new BusinessException(MSG_PAYMENT_NOT_APPROVED);
        }

        String credentials = encryptionService.decrypt(transaction.getAnnouncement().getCredentialsEncrypted());

        return new CredentialsResponseDTO(credentials);
    }
}