package com.lootsafe.dto.response;

import com.lootsafe.enums.UserRole;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String name,
        String email,
        String pixKey,
        Instant createdAt,
        Instant updatedAt,
        Set<UserRole> roles
) {
}
