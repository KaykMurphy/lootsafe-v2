package com.lootsafe.mapper;

import com.lootsafe.dto.response.TransactionResponseDTO;
import com.lootsafe.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "announcementId", source = "announcement.id")
    @Mapping(target = "announcementTitle", source = "announcement.title")
    @Mapping(target = "buyerId", source = "buyer.id")
    @Mapping(target = "buyerName", source = "buyer.name")
    @Mapping(target = "sellerId", source = "seller.id")
    @Mapping(target = "sellerName", source = "seller.name")
    @Mapping(target = "payment", ignore = true)
    TransactionResponseDTO toResponse(Transaction transaction);

}