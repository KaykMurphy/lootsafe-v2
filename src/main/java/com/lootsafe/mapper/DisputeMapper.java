package com.lootsafe.mapper;

import com.lootsafe.dto.response.DisputeResponseDTO;
import com.lootsafe.entity.DisputeChat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DisputeMapper {

    @Mapping(target = "transactionId", source = "transaction.id")
    @Mapping(target = "initiatedById", source = "initiatedBy.id")
    @Mapping(target = "initiatedByName", source = "initiatedBy.name")
    DisputeResponseDTO toResponse(DisputeChat disputeChat);

}
