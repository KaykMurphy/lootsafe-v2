package com.lootsafe.dto.request;

import jakarta.validation.constraints.*;

public record UserRequestDTO(

        @NotBlank
        String name,

        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String passwordHash,

        @Size(max = 255)
        String pixKey



){
}