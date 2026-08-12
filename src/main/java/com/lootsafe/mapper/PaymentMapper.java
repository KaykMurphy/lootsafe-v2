package com.lootsafe.mapper;

import com.lootsafe.dto.response.PaymentResponseDTO;
import com.lootsafe.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "transactionId", source = "transaction.id")
    PaymentResponseDTO toResponse(Payment payment);

}