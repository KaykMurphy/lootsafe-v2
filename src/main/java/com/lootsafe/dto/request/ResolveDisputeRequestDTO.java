package com.lootsafe.dto.request;

import com.lootsafe.enums.DisputeStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveDisputeRequestDTO(

        @NotNull
        DisputeStatus resolutionStatus,

        @Size(max = 1000)
        String resolutionNotes

) {
}