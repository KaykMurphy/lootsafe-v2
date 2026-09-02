package com.lootsafe.payment.payout;

import com.lootsafe.enums.PayoutStatus;

import java.time.Instant;

public record PayoutResult(

        String externalTransferId,
        PayoutStatus status,
        Instant processedAt,
        String rawResponse,
        String errorMessage

) {
}
