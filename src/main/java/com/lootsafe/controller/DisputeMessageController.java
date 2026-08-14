package com.lootsafe.controller;

import com.lootsafe.dto.request.DisputeMessageRequestDTO;
import com.lootsafe.dto.response.DisputeMessageResponseDTO;
import com.lootsafe.service.DisputeMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/disputes/{disputeId}/messages")
public class DisputeMessageController {

    private final DisputeMessageService disputeMessageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DisputeMessageResponseDTO sendMessage(
            @PathVariable UUID disputeId,
            @RequestBody @Valid DisputeMessageRequestDTO request,
            @AuthenticationPrincipal UUID currentUserId) {

        return disputeMessageService.sendMessage(disputeId, currentUserId, request.content());
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<DisputeMessageResponseDTO> listMessages(
            @PathVariable UUID disputeId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        return disputeMessageService.listMessages(disputeId, currentUserId);
    }

}
