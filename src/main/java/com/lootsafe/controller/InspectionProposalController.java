package com.lootsafe.controller;

import com.lootsafe.dto.request.InspectionProposalRequest;
import com.lootsafe.dto.response.InspectionProposalResponseDTO;
import com.lootsafe.service.InspectionNegotiationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InspectionProposalController {

    private final InspectionNegotiationService inspectionNegotiationService;

    @PostMapping("/announcements/{announcementId}/proposals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUYER')")
    public InspectionProposalResponseDTO createProposal(
            @PathVariable UUID announcementId,
            @RequestBody @Valid InspectionProposalPayload payload,
            @AuthenticationPrincipal UUID currentUserId) {
        InspectionProposalRequest request = new InspectionProposalRequest(
                announcementId,
                payload.proposedHours(),
                payload.proposalMessage()
        );
        return inspectionNegotiationService.createProposal(currentUserId, request);
    }

    @GetMapping("/announcements/{announcementId}/proposals")
    public List<InspectionProposalResponseDTO> getAnnouncementProposals(
            @PathVariable UUID announcementId,
            @AuthenticationPrincipal UUID currentUserId) {
        return inspectionNegotiationService.listProposalsByAnnouncement(announcementId, currentUserId);
    }

    @PostMapping("/proposals/{proposalId}/accept")
    @PreAuthorize("hasRole('SELLER')")
    public InspectionProposalResponseDTO acceptProposal(
            @PathVariable UUID proposalId,
            @AuthenticationPrincipal UUID currentUserId) {
        return inspectionNegotiationService.acceptProposal(proposalId, currentUserId);
    }

    @PostMapping("/proposals/{proposalId}/reject")
    @PreAuthorize("hasRole('SELLER')")
    public InspectionProposalResponseDTO rejectProposal(
            @PathVariable UUID proposalId,
            @AuthenticationPrincipal UUID currentUserId) {
        return inspectionNegotiationService.rejectProposal(proposalId, currentUserId);
    }

    public record InspectionProposalPayload(
            Integer proposedHours,
            String proposalMessage
    ) {}
}
