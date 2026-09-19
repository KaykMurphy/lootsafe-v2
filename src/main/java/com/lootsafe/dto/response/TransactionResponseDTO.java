package com.lootsafe.dto.response;

import com.lootsafe.enums.PayoutStatus;
import com.lootsafe.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponseDTO(

        UUID id,
        UUID announcementId,
        String announcementTitle,
        UUID buyerId,
        String buyerName,
        UUID sellerId,
        String sellerName,
        TransactionStatus status,
        BigDecimal amount,
        BigDecimal platformFee,
        BigDecimal netAmount,
        Instant createdAt,
        Instant updatedAt,
        PaymentResponseDTO payment,

        Integer inspectionTimeHours,
        Instant inspectionExpiresAt,
        PayoutStatus payoutStatus,
        String payoutPaidAt

) {
}