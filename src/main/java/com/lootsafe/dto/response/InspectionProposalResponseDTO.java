package com.lootsafe.dto.response;

import com.lootsafe.enums.ProposalStatus;

import java.time.Instant;
import java.util.UUID;

public record InspectionProposalResponseDTO(
        UUID id,
        UUID announcementId,
        UUID buyerId,
        UUID sellerId,
        Integer proposedHours,
        String proposalMessage,
        ProposalStatus status,
        String authorizationToken,
        Instant createdAt,
        Instant updatedAt
) {
}
