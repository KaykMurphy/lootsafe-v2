package com.lootsafe.service;

import com.lootsafe.payment.service.MercadoPagoClient;
import com.lootsafe.repository.AnnouncementRepository;
import com.lootsafe.repository.PaymentRepository;
import com.lootsafe.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class PaymenetExpirationService {

    private PaymentRepository paymentRepository;
    private TransactionRepository transactionRepository;
    private AnnouncementRepository announcementRepository;
    private MercadoPagoClient mercadoPagoClient;

    

}
