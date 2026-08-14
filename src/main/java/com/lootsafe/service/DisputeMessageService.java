package com.lootsafe.service;

import com.lootsafe.dto.response.DisputeMessageResponseDTO;
import com.lootsafe.entity.DisputeChat;
import com.lootsafe.entity.DisputeMessage;
import com.lootsafe.entity.Transaction;
import com.lootsafe.entity.User;
import com.lootsafe.enums.UserRole;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.exception.UnauthorizedException;
import com.lootsafe.mapper.DisputeMessageMapper;
import com.lootsafe.repository.DisputeMessageRepository;
import com.lootsafe.repository.DisputeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
@Transactional(readOnly = true)
public class DisputeMessageService {

    private final DisputeMessageRepository disputeMessageRepository;
    private final DisputeRepository disputeRepository;
    private final UserService userService;
    private final DisputeService disputeService;
    private final DisputeMessageMapper disputeMessageMapper;

    private static final String MSG_DISPUTE_NOT_FOUND = "Disputa não encontrada.";
    private static final String MSG_NOT_DISPUTE_PARTICIPANT =
            "Apenas o comprador, o vendedor ou um administrador podem enviar mensagens nesta disputa.";


    @Transactional
    public DisputeMessageResponseDTO sendMessage(UUID disputeId, UUID senderId,
                                                 String content){

        DisputeChat disputeChat = findDisputeChatById(disputeId);

        User sender = userService.findEntityById(senderId);

        validateParticipant(disputeChat, sender);

        DisputeMessage disputeMessage = new DisputeMessage();
        disputeMessage.setDisputeChat(disputeChat);
        disputeMessage.setSender(sender);
        disputeMessage.setContent(content);


        DisputeMessage savedMessage = disputeMessageRepository.save(disputeMessage);

        return disputeMessageMapper.toResponse(savedMessage);
    }


    public List<DisputeMessageResponseDTO> listMessages(UUID disputeId, UUID userId){

        DisputeChat disputeChat = findDisputeChatById(disputeId);


        User user = userService.findEntityById(userId);
        validateParticipant(disputeChat, user);

        List<DisputeMessage> messages = disputeMessageRepository
                .findByDisputeChatIdOrderByCreatedAtAsc(disputeId);

        return messages.stream()
                .map(message -> disputeMessageMapper.toResponse(message))
                .toList();

    }


    private DisputeChat findDisputeChatById(UUID disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_DISPUTE_NOT_FOUND));
    }

    private void validateParticipant(DisputeChat disputeChat, User user) {
        Transaction transaction = disputeChat.getTransaction();

        boolean isBuyer = transaction.getBuyer().getId().equals(user.getId());
        boolean isSeller = transaction.getSeller().getId().equals(user.getId());
        boolean isAdmin = user.hasRole(UserRole.ADMIN);

        if (!isBuyer && !isSeller && !isAdmin) {
            throw new UnauthorizedException(MSG_NOT_DISPUTE_PARTICIPANT);
        }
    }

}
