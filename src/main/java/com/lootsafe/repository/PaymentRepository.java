package com.lootsafe.repository;

import com.lootsafe.entity.Payment;
import com.lootsafe.entity.Transaction;
import com.lootsafe.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByExternalId(String externalId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByExternalReference(String externalReference);

    List<Payment> findByTransactionId(UUID transactionId);

    List<Payment> findByStatusAndExpiresAtBefore(PaymentStatus status, Instant now);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByTransactionIdAndStatus(UUID transactionId, PaymentStatus status);

}
