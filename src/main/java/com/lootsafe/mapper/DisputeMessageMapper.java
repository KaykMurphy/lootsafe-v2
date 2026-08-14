package com.lootsafe.mapper;

import com.lootsafe.dto.response.DisputeMessageResponseDTO;
import com.lootsafe.entity.DisputeMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DisputeMessageMapper {

    DisputeMessageResponseDTO toResponse(DisputeMessage disputeMessage);

}
