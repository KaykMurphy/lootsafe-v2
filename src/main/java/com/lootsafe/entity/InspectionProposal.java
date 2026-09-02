package com.lootsafe.entity;

import com.lootsafe.enums.ProposalStatus;
import com.lootsafe.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inspection_proposals")
public class InspectionProposal extends AbstractAuditableEntity {

    private static final String MSG_ONLY_PENDING_CAN_BE_ACCEPTED =
            "Apenas propostas pendentes podem ser aceitas.";
    private static final String MSG_ONLY_PENDING_CAN_BE_REJECTED =
            "Apenas propostas pendentes podem ser rejeitadas.";
    private static final String MSG_TOKEN_CANNOT_BE_BLANK =
            "O token de autorização não pode ser nulo ou vazio.";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(name = "proposed_hours", nullable = false)
    private Integer proposedHours;

    @Column(name = "proposal_message", columnDefinition = "TEXT")
    private String proposalMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProposalStatus status = ProposalStatus.PENDING;

    @Column(name = "authorization_token")
    private String authorizationToken;

    public boolean isPending() {
        return this.status == ProposalStatus.PENDING;
    }

    public void accept(String token) {
        if (!isPending()) {
            throw new BusinessException(MSG_ONLY_PENDING_CAN_BE_ACCEPTED);
        }
        if (token == null || token.isBlank()) {
            throw new BusinessException(MSG_TOKEN_CANNOT_BE_BLANK);
        }
        this.status = ProposalStatus.ACCEPTED;
        this.authorizationToken = token;
    }

    public void reject() {
        if (!isPending()) {
            throw new BusinessException(MSG_ONLY_PENDING_CAN_BE_REJECTED);
        }
        this.status = ProposalStatus.REJECTED;
    }

    public void expire() {
        if (isPending()) {
            this.status = ProposalStatus.EXPIRED;
        }
    }
}
