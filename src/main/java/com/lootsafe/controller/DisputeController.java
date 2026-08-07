package com.lootsafe.controller;

import com.lootsafe.dto.request.DisputeRequestDTO;
import com.lootsafe.dto.request.ResolveDisputeRequestDTO;
import com.lootsafe.dto.response.DisputeResponseDTO;
import com.lootsafe.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DisputeResponseDTO openDispute(@RequestBody @Valid DisputeRequestDTO request,
                                          @AuthenticationPrincipal UUID currentUserId) {
        return disputeService.openDispute(request.transactionId(), currentUserId, request.reason());
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public DisputeResponseDTO resolveDispute(@PathVariable UUID id,
                                             @RequestBody @Valid ResolveDisputeRequestDTO request) {
        return disputeService.resolveDispute(id, request.resolutionStatus(), request.resolutionNotes());
    }
}