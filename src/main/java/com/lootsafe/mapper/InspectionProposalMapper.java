package com.lootsafe.mapper;

import com.lootsafe.dto.response.InspectionProposalResponseDTO;
import com.lootsafe.entity.InspectionProposal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InspectionProposalMapper {

    @Mapping(source = "announcement.id", target = "announcementId")
    @Mapping(source = "buyer.id", target = "buyerId")
    @Mapping(source = "announcement.seller.id", target = "sellerId")
    InspectionProposalResponseDTO toResponse(InspectionProposal proposal);

}
