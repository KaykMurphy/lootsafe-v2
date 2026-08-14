package com.lootsafe.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DisputeMessageRequestDTO(

        @NotBlank
        String content

) {
}
