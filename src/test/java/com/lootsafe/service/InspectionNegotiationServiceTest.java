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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InspectionNegotiationServiceTest {

    @Mock
    private InspectionProposalRepository inspectionProposalRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private UserService userService;

    @Mock
    private InspectionProperties inspectionProperties;

    @Mock
    private InspectionProposalMapper inspectionProposalMapper;

    @InjectMocks
    private InspectionNegotiationService inspectionNegotiationService;

    private User seller;
    private User buyer;
    private Announcement announcement;
    private UUID buyerId;
    private UUID sellerId;
    private UUID announcementId;
    private UUID proposalId;

    @BeforeEach
    void setUp() {
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        announcementId = UUID.randomUUID();
        proposalId = UUID.randomUUID();

        seller = new User();
        seller.setId(sellerId);
        seller.setName("Vendedor Teste");
        seller.setEmail("vendedor@lootsafe.com");
        seller.setRoles(Set.of(UserRole.SELLER));

        buyer = new User();
        buyer.setId(buyerId);
        buyer.setName("Comprador Teste");
        buyer.setEmail("comprador@lootsafe.com");
        buyer.setRoles(Set.of(UserRole.BUYER));

        announcement = new Announcement();
        announcement.setId(announcementId);
        announcement.setSeller(seller);
        announcement.setTitle("Item Raro");
        announcement.setStatus(AnnouncementStatus.ACTIVE);
        announcement.setPrice(new BigDecimal("100.00"));
        announcement.setInspectionTimeHours(2);
    }

    @Test
    @DisplayName("Deve criar uma proposta de inspeção com sucesso")
    void createProposal_Success() {
        InspectionProposalRequest request = new InspectionProposalRequest(announcementId, 12, "Gostaria de testar por 12h.");

        when(announcementRepository.findById(announcementId)).thenReturn(Optional.of(announcement));
        when(userService.findEntityById(buyerId)).thenReturn(buyer);
        when(inspectionProperties.getMinHours()).thenReturn(new BigDecimal("1"));
        when(inspectionProperties.getMaxHours()).thenReturn(new BigDecimal("72"));

        InspectionProposal savedProposal = new InspectionProposal();
        savedProposal.setId(proposalId);
        savedProposal.setAnnouncement(announcement);
        savedProposal.setBuyer(buyer);
        savedProposal.setProposedHours(12);
        savedProposal.setProposalMessage("Gostaria de testar por 12h.");
        savedProposal.setStatus(ProposalStatus.PENDING);

        when(inspectionProposalRepository.save(any(InspectionProposal.class))).thenReturn(savedProposal);

        InspectionProposalResponseDTO responseDTO = new InspectionProposalResponseDTO(
                proposalId, announcementId, buyerId, sellerId, 12, "Gostaria de testar por 12h.",
                ProposalStatus.PENDING, null, null, null
        );
        when(inspectionProposalMapper.toResponse(savedProposal)).thenReturn(responseDTO);

        InspectionProposalResponseDTO result = inspectionNegotiationService.createProposal(buyerId, request);

        assertThat(result).isNotNull();
        assertThat(result.proposedHours()).isEqualTo(12);
        assertThat(result.status()).isEqualTo(ProposalStatus.PENDING);
        verify(inspectionProposalRepository, times(1)).save(any(InspectionProposal.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando anúncio não for encontrado ao criar proposta")
    void createProposal_AnnouncementNotFound() {
        InspectionProposalRequest request = new InspectionProposalRequest(announcementId, 12, "Teste");
        when(announcementRepository.findById(announcementId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inspectionNegotiationService.createProposal(buyerId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Anúncio não encontrado.");
    }

    @Test
    @DisplayName("Deve lançar exceção quando anúncio não estiver ativo")
    void createProposal_AnnouncementNotActive() {
        announcement.setStatus(AnnouncementStatus.SOLD);
        InspectionProposalRequest request = new InspectionProposalRequest(announcementId, 12, "Teste");
        when(announcementRepository.findById(announcementId)).thenReturn(Optional.of(announcement));

        assertThatThrownBy(() -> inspectionNegotiationService.createProposal(buyerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O anúncio não está mais disponível para negociação.");
    }

    @Test
    @DisplayName("Deve lançar exceção quando vendedor tenta propor para o próprio anúncio")
    void createProposal_CannotProposeToOwnAnnouncement() {
        InspectionProposalRequest request = new InspectionProposalRequest(announcementId, 12, "Teste");
        when(announcementRepository.findById(announcementId)).thenReturn(Optional.of(announcement));

        assertThatThrownBy(() -> inspectionNegotiationService.createProposal(sellerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Você não pode enviar uma proposta para o seu próprio anúncio.");
    }

    @Test
    @DisplayName("Deve lançar exceção quando horas propostas estiverem fora dos limites")
    void createProposal_HoursOutOfBounds() {
        InspectionProposalRequest request = new InspectionProposalRequest(announcementId, 100, "Teste");
        when(announcementRepository.findById(announcementId)).thenReturn(Optional.of(announcement));
        when(userService.findEntityById(buyerId)).thenReturn(buyer);
        when(inspectionProperties.getMinHours()).thenReturn(new BigDecimal("1"));
        when(inspectionProperties.getMaxHours()).thenReturn(new BigDecimal("72"));

        assertThatThrownBy(() -> inspectionNegotiationService.createProposal(buyerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("O tempo de inspeção proposto deve estar entre 1 e 72 horas.");
    }

    @Test
    @DisplayName("Deve aceitar proposta com sucesso e gerar token de autorização")
    void acceptProposal_Success() {
        InspectionProposal proposal = new InspectionProposal();
        proposal.setId(proposalId);
        proposal.setAnnouncement(announcement);
        proposal.setBuyer(buyer);
        proposal.setProposedHours(10);
        proposal.setStatus(ProposalStatus.PENDING);

        when(inspectionProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(inspectionProposalRepository.save(proposal)).thenReturn(proposal);

        InspectionProposalResponseDTO responseDTO = new InspectionProposalResponseDTO(
                proposalId, announcementId, buyerId, sellerId, 10, null,
                ProposalStatus.ACCEPTED, "auth-token-123", null, null
        );
        when(inspectionProposalMapper.toResponse(proposal)).thenReturn(responseDTO);

        InspectionProposalResponseDTO result = inspectionNegotiationService.acceptProposal(proposalId, sellerId);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ProposalStatus.ACCEPTED);
        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
        assertThat(proposal.getAuthorizationToken()).isNotNull();
        verify(inspectionProposalRepository).save(proposal);
    }

    @Test
    @DisplayName("Deve lançar exceção ao aceitar proposta se usuário não for o vendedor do anúncio")
    void acceptProposal_UnauthorizedSeller() {
        InspectionProposal proposal = new InspectionProposal();
        proposal.setId(proposalId);
        proposal.setAnnouncement(announcement);
        proposal.setBuyer(buyer);
        proposal.setStatus(ProposalStatus.PENDING);

        when(inspectionProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        UUID anotherUserId = UUID.randomUUID();
        assertThatThrownBy(() -> inspectionNegotiationService.acceptProposal(proposalId, anotherUserId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Apenas o vendedor do anúncio pode responder a esta proposta.");
    }

    @Test
    @DisplayName("Deve lançar exceção ao aceitar proposta que não está pendente")
    void acceptProposal_NotPending() {
        InspectionProposal proposal = new InspectionProposal();
        proposal.setId(proposalId);
        proposal.setAnnouncement(announcement);
        proposal.setBuyer(buyer);
        proposal.setStatus(ProposalStatus.ACCEPTED);

        when(inspectionProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> inspectionNegotiationService.acceptProposal(proposalId, sellerId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Apenas propostas pendentes podem ser respondidas.");
    }

    @Test
    @DisplayName("Deve rejeitar proposta com sucesso")
    void rejectProposal_Success() {
        InspectionProposal proposal = new InspectionProposal();
        proposal.setId(proposalId);
        proposal.setAnnouncement(announcement);
        proposal.setBuyer(buyer);
        proposal.setProposedHours(10);
        proposal.setStatus(ProposalStatus.PENDING);

        when(inspectionProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(inspectionProposalRepository.save(proposal)).thenReturn(proposal);

        InspectionProposalResponseDTO responseDTO = new InspectionProposalResponseDTO(
                proposalId, announcementId, buyerId, sellerId, 10, null,
                ProposalStatus.REJECTED, null, null, null
        );
        when(inspectionProposalMapper.toResponse(proposal)).thenReturn(responseDTO);

        InspectionProposalResponseDTO result = inspectionNegotiationService.rejectProposal(proposalId, sellerId);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ProposalStatus.REJECTED);
        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.REJECTED);
    }

    @Test
    @DisplayName("Deve obter proposta por ID para comprador, vendedor ou admin")
    void getProposalById_Success() {
        InspectionProposal proposal = new InspectionProposal();
        proposal.setId(proposalId);
        proposal.setAnnouncement(announcement);
        proposal.setBuyer(buyer);
        proposal.setStatus(ProposalStatus.PENDING);

        when(inspectionProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(userService.findEntityById(buyerId)).thenReturn(buyer);

        InspectionProposalResponseDTO responseDTO = new InspectionProposalResponseDTO(
                proposalId, announcementId, buyerId, sellerId, 10, null,
                ProposalStatus.PENDING, null, null, null
        );
        when(inspectionProposalMapper.toResponse(proposal)).thenReturn(responseDTO);

        InspectionProposalResponseDTO result = inspectionNegotiationService.getProposalById(proposalId, buyerId);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Deve lançar exceção se usuário não autorizado tentar ver proposta")
    void getProposalById_Unauthorized() {
        InspectionProposal proposal = new InspectionProposal();
        proposal.setId(proposalId);
        proposal.setAnnouncement(announcement);
        proposal.setBuyer(buyer);
        proposal.setStatus(ProposalStatus.PENDING);

        UUID strangerId = UUID.randomUUID();
        User stranger = new User();
        stranger.setId(strangerId);
        stranger.setRoles(Set.of(UserRole.BUYER));

        when(inspectionProposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(userService.findEntityById(strangerId)).thenReturn(stranger);

        assertThatThrownBy(() -> inspectionNegotiationService.getProposalById(proposalId, strangerId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Você não tem permissão para acessar esta proposta.");
    }

    @Test
    @DisplayName("Deve validar token de autorização de compra com sucesso")
    void validateProposalAuthorization_Success() {
        String token = "valid-auth-token";
        InspectionProposal proposal = new InspectionProposal();
        proposal.setId(proposalId);
        proposal.setAnnouncement(announcement);
        proposal.setBuyer(buyer);
        proposal.setProposedHours(8);
        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setAuthorizationToken(token);

        when(inspectionProposalRepository.findByAuthorizationToken(token)).thenReturn(Optional.of(proposal));

        Integer agreedHours = inspectionNegotiationService.validateProposalAuthorization(token, buyerId, announcementId);

        assertThat(agreedHours).isEqualTo(8);
    }

    @Test
    @DisplayName("Deve lançar exceção quando token de autorização for inválido")
    void validateProposalAuthorization_InvalidToken() {
        when(inspectionProposalRepository.findByAuthorizationToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inspectionNegotiationService.validateProposalAuthorization("invalid", buyerId, announcementId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Token de autorização de compra inválido ou expirado.");
    }
}
