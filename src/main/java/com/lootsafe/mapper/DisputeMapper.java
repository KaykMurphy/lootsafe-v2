package com.lootsafe.mapper;

import com.lootsafe.dto.response.DisputeResponseDTO;
import com.lootsafe.entity.DisputeChat;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DisputeMapper {

    DisputeResponseDTO toResponse(DisputeChat disputeChat);

}
