package com.lootsafe.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DisputeMessageResponseDTO(

        UUID id,
        UUID disputeChatId,
        UUID disputeId,
        UUID senderId,
        String senderName,
        String content,
        Instant createdAt

) {
}
