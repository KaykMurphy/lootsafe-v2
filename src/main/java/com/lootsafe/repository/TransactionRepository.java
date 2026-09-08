package com.lootsafe.repository;

import com.lootsafe.entity.Transaction;
import com.lootsafe.enums.PayoutStatus;
import com.lootsafe.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findById(UUID id);

    Optional<Transaction> findFirstByAnnouncementIdOrderByCreatedAtDesc(UUID announcementId);

    Optional<Transaction> findFirstByAnnouncementIdAndStatus(UUID announcementId, TransactionStatus status);

    List<Transaction> findByAnnouncementId(UUID announcementId);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByBuyerId(UUID buyerId);

    List<Transaction> findBySellerId(UUID sellerId);

    List<Transaction> findByStatusAndInspectionExpiresAtBefore(
            TransactionStatus status, Instant expirationLimit
    );

    List<Transaction> findByStatusAndPayoutStatusIn(TransactionStatus status,
                                                    Collection<PayoutStatus> payoutStatuses);




}
