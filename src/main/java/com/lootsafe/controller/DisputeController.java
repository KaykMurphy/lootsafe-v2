package com.lootsafe.controller;

import com.lootsafe.dto.request.DisputeRequestDTO;
import com.lootsafe.dto.request.ResolveDisputeRequestDTO;
import com.lootsafe.dto.response.DisputeResponseDTO;
import com.lootsafe.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DisputeResponseDTO openDispute(
            @RequestHeader("X-User-Id") UUID initiatedById,
            @RequestBody @Valid DisputeRequestDTO request) {
        return disputeService.openDispute(request.transactionId(), initiatedById, request.reason());
    }

    @PutMapping("/{id}/resolve")
    public DisputeResponseDTO resolveDispute(
            @PathVariable UUID id,
            @RequestBody @Valid ResolveDisputeRequestDTO request) {
        return disputeService.resolveDispute(id, request.resolutionStatus(), request.resolutionNotes());
    }
}