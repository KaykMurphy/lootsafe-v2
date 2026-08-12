package com.lootsafe.dto.response;

import com.lootsafe.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponseDTO(

        UUID id,
        UUID announcementId,
        UUID buyerId,
        UUID sellerId,
        TransactionStatus status,
        BigDecimal amount,
        Instant createdAt,
        Instant updatedAt,
        PaymentResponseDTO payment

) {
}