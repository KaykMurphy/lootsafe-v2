package com.lootsafe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequestDTO(

        @NotBlank
        @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]+$", message = "O nome deve conter apenas letras e espaços.")
        String name,

        @Size(max = 255)
        @Pattern(regexp = "^[a-zA-Z0-9@.\\-_]+$", message = "A chave PIX contém caracteres inválidos.")
        String pixKey

) {
}
