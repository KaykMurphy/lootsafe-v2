package com.lootsafe.service;

import com.lootsafe.entity.Announcement;
import com.lootsafe.entity.Payment;
import com.lootsafe.entity.Transaction;
import com.lootsafe.enums.AnnouncementStatus;
import com.lootsafe.enums.PaymentStatus;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.payment.service.MercadoPagoClient;
import com.lootsafe.repository.AnnouncementRepository;
import com.lootsafe.repository.PaymentRepository;
import com.lootsafe.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class PaymentExpirationService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final AnnouncementRepository announcementRepository;
    private final MercadoPagoClient mercadoPagoClient;

    @Transactional
    public void expirePendingPayments() {

        List<Payment> expiredPayments = paymentRepository.findByStatusAndExpiresAtBefore(
                PaymentStatus.PENDING, Instant.now()
        );

        for (Payment payment : expiredPayments){

            if (payment.getExternalId() != null) {
                mercadoPagoClient.cancelOrder(payment.getExternalId());
            }

            payment.setStatus(PaymentStatus.EXPIRED);

            paymentRepository.save(payment);

            Transaction transaction = payment.getTransaction();

            if (transaction != null && transaction.getStatus() == TransactionStatus.PENDING){
                transaction.setStatus(TransactionStatus.CANCELLED);
                transactionRepository.save(transaction);
            }

            Announcement announcement = transaction.getAnnouncement();

            if (announcement != null && announcement.getStatus() == AnnouncementStatus.RESERVED){

                announcement.setStatus(AnnouncementStatus.ACTIVE);
                announcementRepository.save(announcement);

            }

        }



    }

}
