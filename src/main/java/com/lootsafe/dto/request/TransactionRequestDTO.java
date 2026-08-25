package com.lootsafe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TransactionRequestDTO(

        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9\\-]+$", message = "O token do anúncio possui formato inválido.") // '@' adicionado
        String announcementToken

) {
}