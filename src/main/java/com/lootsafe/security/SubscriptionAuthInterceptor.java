package com.lootsafe.security;

import com.lootsafe.entity.DisputeChat;
import com.lootsafe.entity.Transaction;
import com.lootsafe.entity.User;
import com.lootsafe.enums.UserRole;
import com.lootsafe.repository.DisputeRepository;
import com.lootsafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interceptor de canal STOMP que autoriza inscrições em tópicos de disputa.
 *
 * Garante que apenas o comprador, o vendedor ou um administrador da transação
 * vinculada à disputa possam se inscrever no tópico
 * {@code /topic/disputes/{disputeId}/messages}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionAuthInterceptor implements ChannelInterceptor {

    private static final Pattern DISPUTE_TOPIC_PATTERN =
            Pattern.compile("^/topic/disputes/([0-9a-fA-F\\-]{36})/messages$");

    private static final String MSG_AUTH_REQUIRED =
            "Autenticação necessária para se inscrever neste tópico.";
    private static final String MSG_NOT_PARTICIPANT =
            "Você não tem permissão para acessar as mensagens desta disputa.";
    private static final String MSG_DISPUTE_NOT_FOUND =
            "Disputa não encontrada.";

    private final DisputeRepository disputeRepository;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }

        Matcher matcher = DISPUTE_TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return message;
        }

        Principal principal = accessor.getUser();

        if (principal == null) {
            log.warn(
                    "Inscrição STOMP rejeitada: Principal ausente no frame SUBSCRIBE para {}",
                    destination
            );

            throw new MessageDeliveryException(MSG_AUTH_REQUIRED);
        }

        if (principal == null) {
            log.warn("Inscrição STOMP rejeitada: Principal ausente no frame SUBSCRIBE para {}", destination);
            throw new MessageDeliveryException(MSG_AUTH_REQUIRED);
        }

        UUID userId = UUID.fromString(principal.getName());
        UUID disputeId = UUID.fromString(matcher.group(1));

        DisputeChat disputeChat = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new MessageDeliveryException(MSG_DISPUTE_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MessageDeliveryException(MSG_AUTH_REQUIRED));

        Transaction transaction = disputeChat.getTransaction();

        boolean isBuyer = transaction.getBuyer().getId().equals(userId);
        boolean isSeller = transaction.getSeller().getId().equals(userId);
        boolean isAdmin = user.hasRole(UserRole.ADMIN);

        if (!isBuyer && !isSeller && !isAdmin) {
            log.warn("Inscrição STOMP rejeitada: usuário {} tentou acessar disputa {}",
                    userId, disputeId);
            throw new MessageDeliveryException(MSG_NOT_PARTICIPANT);
        }

        log.debug("Inscrição STOMP autorizada: usuário {} na disputa {}", userId, disputeId);
        return message;
    }
}
