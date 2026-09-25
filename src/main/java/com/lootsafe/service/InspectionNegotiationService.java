package com.lootsafe.service;

import com.lootsafe.config.InspectionProperties;
import com.lootsafe.dto.request.InspectionProposalRequest;
import com.lootsafe.dto.response.InspectionProposalResponseDTO;
import com.lootsafe.entity.Announcement;
import com.lootsafe.entity.InspectionProposal;
import com.lootsafe.entity.User;
import com.lootsafe.enums.AnnouncementStatus;
import com.lootsafe.enums.ProposalStatus;
import com.lootsafe.enums.UserRole;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.exception.UnauthorizedException;
import com.lootsafe.mapper.InspectionProposalMapper;
import com.lootsafe.repository.AnnouncementRepository;
import com.lootsafe.repository.InspectionProposalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionNegotiationService {

    private static final String MSG_ANNOUNCEMENT_NOT_FOUND = "Anúncio não encontrado.";
    private static final String MSG_BUYER_NOT_FOUND = "Usuário comprador não encontrado.";
    private static final String MSG_CANNOT_PROPOSE_TO_OWN_ANNOUNCEMENT =
            "Você não pode enviar uma proposta para o seu próprio anúncio.";
    private static final String MSG_PROPOSED_HOURS_OUT_OF_BOUNDS =
            "O tempo de inspeção proposto deve estar entre %s e %s horas.";
    private static final String MSG_PROPOSAL_NOT_FOUND = "Proposta de inspeção não encontrada.";
    private static final String MSG_UNAUTHORIZED_SELLER =
            "Apenas o vendedor do anúncio pode responder a esta proposta.";
    private static final String MSG_PROPOSAL_NOT_PENDING =
            "Apenas propostas pendentes podem ser respondidas.";
    private static final String MSG_ANNOUNCEMENT_NOT_ACTIVE =
            "O anúncio não está mais disponível para negociação.";
    private static final String MSG_UNAUTHORIZED_ACCESS =
            "Você não tem permissão para acessar esta proposta.";
    private static final String MSG_INVALID_AUTHORIZATION_TOKEN =
            "Token de autorização de compra inválido ou expirado.";
    private static final String MSG_TOKEN_BUYER_MISMATCH =
            "O token de autorização não pertence ao comprador informado.";
    private static final String MSG_TOKEN_ANNOUNCEMENT_MISMATCH =
            "O token de autorização não pertence ao anúncio informado.";

    private final InspectionProposalRepository inspectionProposalRepository;
    private final AnnouncementRepository announcementRepository;
    private final UserService userService;
    private final InspectionProperties inspectionProperties;
    private final InspectionProposalMapper inspectionProposalMapper;

    @Transactional
    public InspectionProposalResponseDTO createProposal(UUID buyerId, InspectionProposalRequest request) {
        Announcement announcement = announcementRepository.findById(request.announcementId())
                .orElseThrow(() -> new ResourceNotFoundException(MSG_ANNOUNCEMENT_NOT_FOUND));

        if (announcement.getStatus() != AnnouncementStatus.ACTIVE) {
            throw new BusinessException(MSG_ANNOUNCEMENT_NOT_ACTIVE);
        }

        if (announcement.getSeller().getId().equals(buyerId)) {
            throw new BusinessException(MSG_CANNOT_PROPOSE_TO_OWN_ANNOUNCEMENT);
        }

        User buyer = userService.findEntityById(buyerId);

        BigDecimal proposedHoursBd = BigDecimal.valueOf(request.proposedHours());
        if (proposedHoursBd.compareTo(inspectionProperties.getMinHours()) < 0
                || proposedHoursBd.compareTo(inspectionProperties.getMaxHours()) > 0) {
            throw new BusinessException(String.format(
                    MSG_PROPOSED_HOURS_OUT_OF_BOUNDS,
                    inspectionProperties.getMinHours(),
                    inspectionProperties.getMaxHours()
            ));
        }

        InspectionProposal proposal = new InspectionProposal();
        proposal.setAnnouncement(announcement);
        proposal.setBuyer(buyer);
        proposal.setProposedHours(request.proposedHours());
        proposal.setProposalMessage(request.proposalMessage());
        proposal.setStatus(ProposalStatus.PENDING);

        InspectionProposal savedProposal = inspectionProposalRepository.save(proposal);

        log.info("Proposta de tempo de inspeção criada: id={}, announcementId={}, buyerId={}, proposedHours={}h",
                savedProposal.getId(), announcement.getId(), buyerId, request.proposedHours());

        return inspectionProposalMapper.toResponse(savedProposal);
    }

    @Transactional
    public InspectionProposalResponseDTO acceptProposal(UUID proposalId, UUID sellerId) {
        InspectionProposal proposal = findEntityById(proposalId);

        if (!proposal.getAnnouncement().getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException(MSG_UNAUTHORIZED_SELLER);
        }

        if (!proposal.isPending()) {
            throw new BusinessException(MSG_PROPOSAL_NOT_PENDING);
        }

        if (proposal.getAnnouncement().getStatus() != AnnouncementStatus.ACTIVE) {
            throw new BusinessException(MSG_ANNOUNCEMENT_NOT_ACTIVE);
        }

        String authorizationToken = UUID.randomUUID().toString();
        proposal.accept(authorizationToken);

        Announcement announcement = proposal.getAnnouncement();
        announcement.setInspectionTimeHours(proposal.getProposedHours());
        announcementRepository.save(announcement);

        InspectionProposal savedProposal = inspectionProposalRepository.save(proposal);

        log.info("Proposta de tempo de inspeção aceita pelo vendedor={}. proposalId={}, announcementId={}, proposedHours={}h, authToken={}",
                sellerId, savedProposal.getId(), savedProposal.getAnnouncement().getId(), savedProposal.getProposedHours(), authorizationToken);

        return inspectionProposalMapper.toResponse(savedProposal);
    }

    @Transactional
    public InspectionProposalResponseDTO rejectProposal(UUID proposalId, UUID sellerId) {
        InspectionProposal proposal = findEntityById(proposalId);

        if (!proposal.getAnnouncement().getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException(MSG_UNAUTHORIZED_SELLER);
        }

        if (!proposal.isPending()) {
            throw new BusinessException(MSG_PROPOSAL_NOT_PENDING);
        }

        proposal.reject();

        InspectionProposal savedProposal = inspectionProposalRepository.save(proposal);

        log.info("Proposta de tempo de inspeção rejeitada pelo vendedor={}. proposalId={}, announcementId={}",
                sellerId, savedProposal.getId(), savedProposal.getAnnouncement().getId());

        return inspectionProposalMapper.toResponse(savedProposal);
    }

    public InspectionProposalResponseDTO getProposalById(UUID proposalId, UUID userId) {
        InspectionProposal proposal = findEntityById(proposalId);
        User user = userService.findEntityById(userId);

        boolean isBuyer = proposal.getBuyer().getId().equals(userId);
        boolean isSeller = proposal.getAnnouncement().getSeller().getId().equals(userId);
        boolean isAdmin = user.hasRole(UserRole.ADMIN);

        if (!isBuyer && !isSeller && !isAdmin) {
            throw new UnauthorizedException(MSG_UNAUTHORIZED_ACCESS);
        }

        return inspectionProposalMapper.toResponse(proposal);
    }

    public List<InspectionProposalResponseDTO> listProposalsByAnnouncement(UUID announcementId, UUID currentUserId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_ANNOUNCEMENT_NOT_FOUND));

        if (announcement.getSeller().getId().equals(currentUserId)) {
            return inspectionProposalRepository.findByAnnouncementId(announcementId).stream()
                    .map(inspectionProposalMapper::toResponse)
                    .toList();
        }

        return inspectionProposalRepository.findByAnnouncementId(announcementId).stream()
                .filter(proposal -> proposal.getBuyer().getId().equals(currentUserId))
                .map(inspectionProposalMapper::toResponse)
                .toList();
    }


    public List<InspectionProposalResponseDTO> listProposalsByBuyer(UUID buyerId) {
        return inspectionProposalRepository.findByBuyerId(buyerId).stream()
                .map(inspectionProposalMapper::toResponse)
                .toList();
    }

    public Integer validateProposalAuthorization(String authorizationToken, UUID buyerId, UUID announcementId) {
        InspectionProposal proposal = inspectionProposalRepository.findByAuthorizationToken(authorizationToken)
                .orElseThrow(() -> new BusinessException(MSG_INVALID_AUTHORIZATION_TOKEN));

        if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
            throw new BusinessException(MSG_INVALID_AUTHORIZATION_TOKEN);
        }

        if (!proposal.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException(MSG_TOKEN_BUYER_MISMATCH);
        }

        if (!proposal.getAnnouncement().getId().equals(announcementId)) {
            throw new BusinessException(MSG_TOKEN_ANNOUNCEMENT_MISMATCH);
        }

        return proposal.getProposedHours();
    }

    public InspectionProposal findEntityById(UUID id) {
        return inspectionProposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROPOSAL_NOT_FOUND));
    }
}