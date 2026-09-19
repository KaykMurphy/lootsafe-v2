package com.lootsafe.controller;

import com.lootsafe.dto.request.DisputeMessageRequestDTO;
import com.lootsafe.exception.UnauthorizedException;
import com.lootsafe.service.DisputeMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * Controller STOMP para mensagens de disputa em tempo real.
 *
 * Recebe mensagens publicadas pelo cliente em {@code /app/disputes/{disputeId}/messages},
 * persiste e transmite via {@link DisputeMessageService} para todos os participantes.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class StompDisputeMessageController {

    private final DisputeMessageService disputeMessageService;

    @MessageMapping("/disputes/{disputeId}/messages")
    public void sendMessage(@DestinationVariable UUID disputeId,
                            @Payload DisputeMessageRequestDTO request,
                            Principal principal) {

        if (principal == null) {
            log.warn("Mensagem STOMP rejeitada: usuário não autenticado.");
            throw new UnauthorizedException(
                    "Usuário não autenticado para envio de mensagem STOMP."
            );
        }

        UUID senderId = UUID.fromString(principal.getName());

        log.debug(
                "Mensagem STOMP recebida: disputa={}, sender={}",
                disputeId,
                senderId
        );

        disputeMessageService.sendMessage(
                disputeId,
                senderId,
                request.content()
        );
    }
}
