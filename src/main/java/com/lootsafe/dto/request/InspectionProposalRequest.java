package com.lootsafe.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record InspectionProposalRequest(

        @NotNull(message = "O ID do anúncio é obrigatório.")
        UUID announcementId,

        @NotNull(message = "O tempo de inspeção proposto é obrigatório.")
        @Min(value = 1, message = "O tempo de inspeção proposto deve ser de no mínimo 1 hora.")
        @Max(value = 720, message = "O tempo de inspeção proposto não pode exceder o limite máximo da plataforma.")
        Integer proposedHours,

        @Size(max = 500, message = "A mensagem da proposta deve ter no máximo 500 caracteres.")
        String proposalMessage

) {
}
