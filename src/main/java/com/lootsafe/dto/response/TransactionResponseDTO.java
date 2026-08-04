package com.lootsafe.dto.response;

import com.lootsafe.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponseDTO(

        UUID id,
        UUID announcementId,
        UUID buyerId,
        UUID sellerId,
        String mercadoPagoPaymentId,
        TransactionStatus status,
        BigDecimal amount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt

) {
}
