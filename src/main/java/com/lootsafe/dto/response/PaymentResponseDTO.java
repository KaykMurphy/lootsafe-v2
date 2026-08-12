package com.lootsafe.dto.response;

import com.lootsafe.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponseDTO(
        UUID paymentId,
        UUID transactionId,
        PaymentStatus status,
        BigDecimal amount,
        String pixCode,
        String qrCodeBase64,
        Instant expiresAt
) {}