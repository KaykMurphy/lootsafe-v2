package com.lootsafe.mapper;

import com.lootsafe.dto.response.DisputeMessageResponseDTO;
import com.lootsafe.entity.DisputeMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DisputeMessageMapper {

    @Mapping(target = "disputeChatId", source = "disputeChat.id")
    @Mapping(target = "disputeId", source = "disputeChat.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderName", source = "sender.name")
    DisputeMessageResponseDTO toResponse(DisputeMessage disputeMessage);

}
