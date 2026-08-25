package com.lootsafe.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AnnouncementRequestDTO(

        @NotBlank
        @Pattern(regexp = "^[^<>]*$", message = "Caracteres HTML não são permitidos no título.")
        String title,

        @Size(max = 2000)
        @Pattern(regexp = "^[^<>]*$", message = "Caracteres HTML não são permitidos na descrição.")
        String description,

        @NotBlank
        @Pattern(regexp = "^[^<>]*$", message = "Caracteres HTML não são permitidos nas credenciais.")
        String credentials,

        @Size(max = 1000)
        @NotBlank
        @Pattern(regexp = "^[^<>]*$", message = "Caracteres HTML não são permitidos nas observações.")
        String notes,

        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9@.\\-_]+$", message = "A chave PIX contém caracteres inválidos.")
        String pixKey,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal price

) {
}