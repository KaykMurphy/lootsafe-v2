package com.lootsafe.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequestDTO(
        @NotBlank String refreshToken
) {}