package com.lootsafe.repository;

import com.lootsafe.entity.InspectionProposal;
import com.lootsafe.enums.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InspectionProposalRepository extends JpaRepository<InspectionProposal, UUID> {

    List<InspectionProposal> findByAnnouncementId(UUID announcementId);

    List<InspectionProposal> findByBuyerId(UUID buyerId);

    List<InspectionProposal> findByAnnouncementSellerId(UUID sellerId);

    Optional<InspectionProposal> findByAuthorizationToken(String authorizationToken);

    boolean existsByAnnouncementIdAndBuyerIdAndStatus(UUID announcementId, UUID buyerId, ProposalStatus status);

}
