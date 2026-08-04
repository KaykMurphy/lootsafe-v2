package com.lootsafe.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TransactionRequestDTO(

        @NotBlank
        String announcementToken

) {
}