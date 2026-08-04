package com.lootsafe.dto.response;

import com.lootsafe.enums.DisputeStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DisputeResponseDTO(

        UUID id,
        UUID transactionId,
        UUID initiatedById,
        DisputeStatus status,
        String reason,
        String resolutionNotes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt

) {
}
