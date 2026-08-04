package com.lootsafe.mapper;

import com.lootsafe.dto.response.TransactionResponseDTO;
import com.lootsafe.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionResponseDTO toResponse(Transaction transaction);

}
