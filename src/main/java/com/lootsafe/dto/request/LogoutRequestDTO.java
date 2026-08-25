package com.lootsafe.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequestDTO(

        @NotBlank(message = "O refresh token é obrigatório para o logout.")
        String refreshToken

) {
}
