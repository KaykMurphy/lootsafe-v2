package com.lootsafe.dto.response;

import com.lootsafe.enums.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponseDTO(

        UUID id,
        String name,
        String email,
        String pixKey,
        UserRole role,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt

) {
}
