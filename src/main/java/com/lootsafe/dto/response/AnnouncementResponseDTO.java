package com.lootsafe.dto.response;

import com.lootsafe.enums.AnnouncementStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AnnouncementResponseDTO(

        UUID id,
        String title,
        String description,
        String credentialsEncrypted,
        String notes,
        String pixKey,
        String token,
        BigDecimal price,
        AnnouncementStatus status,
        UUID sellerId,
        Instant createdAt,
        Instant updatedAt

) {
}
