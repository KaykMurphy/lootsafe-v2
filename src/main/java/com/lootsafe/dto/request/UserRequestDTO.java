package com.lootsafe.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

public record UserRequestDTO(

        @NotBlank
        @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]+$", message = "O nome deve conter apenas letras e espaços.")
        String name,

        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,100}$", message = "A senha deve ter no mínimo 8 caracteres, incluindo letras maiúsculas, minúsculas, números e caracteres especiais.")
        @JsonAlias({"password", "passwordHash"})
        String passwordHash,

        @Size(max = 255)
        @Pattern(regexp = "^[a-zA-Z0-9@.\\-_]+$", message = "A chave PIX contém caracteres inválidos.")
        String pixKey

) {
}






