package com.lootsafe.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AnnouncementRequestDTO(

        @NotBlank
        String title,

        @Size(max = 2000)
        String description,

        @NotBlank
        String credentialsEncrypted,

        @Size(max = 1000)
        String notes,

        @NotBlank
        String pixKey,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal price

) {
}