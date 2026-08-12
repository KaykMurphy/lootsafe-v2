package com.lootsafe.repository;

import com.lootsafe.entity.Payment;
import com.lootsafe.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByExternalId(String externalId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByExternalReference(String externalReference);

    List<Payment> findByTransactionId(UUID transactionId);
}
