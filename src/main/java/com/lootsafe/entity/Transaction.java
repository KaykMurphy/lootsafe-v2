package com.lootsafe.entity;

import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Representa o núcleo financeiro da plataforma (Escrow).
 * Atua como o ponto central de conexão entre o anúncio, o comprador,
 * o vendedor e o sistema de mediação/disputas.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")
public class Transaction extends AbstractAuditableEntity{

    private static final String MSG_ONLY_PENDING_CAN_BE_APPROVED =
            "A transação só pode ser aprovada quando está pendente.";
    private static final String MSG_DISPUTE_NOT_ALLOWED_IN_STATE =
            "A transação não pode entrar em disputa neste estado.";
    private static final String MSG_ONLY_DISPUTED_CAN_BE_RELEASED =
            "A transação só pode ser liberada quando está em disputa.";
    private static final String MSG_ONLY_DISPUTED_CAN_BE_REFUNDED =
            "A transação só pode ser reembolsada quando está em disputa.";

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", unique = true, nullable = false)
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @OneToOne(mappedBy = "transaction",
            cascade = CascadeType.ALL, orphanRemoval = true)
    private DisputeChat dispute;

    public boolean isPending() {
        return getStatus() == TransactionStatus.PENDING;
    }

    public boolean isApproved() {
        return getStatus() == TransactionStatus.APPROVED;
    }

    public void approve() {
        if (getStatus() == TransactionStatus.APPROVED) {
            return;
        }
        if (getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException(MSG_ONLY_PENDING_CAN_BE_APPROVED);
        }
        setStatus(TransactionStatus.APPROVED);
    }

    public void markAsDisputed() {
        if (getStatus() == TransactionStatus.DISPUTED) {
            return;
        }
        if (getStatus() != TransactionStatus.PENDING
                && getStatus() != TransactionStatus.APPROVED) {
            throw new BusinessException(MSG_DISPUTE_NOT_ALLOWED_IN_STATE);
        }
        setStatus(TransactionStatus.DISPUTED);
    }

    public void release() {
        if (getStatus() != TransactionStatus.DISPUTED) {
            throw new BusinessException(MSG_ONLY_DISPUTED_CAN_BE_RELEASED);
        }
        setStatus(TransactionStatus.RELEASED);
    }

    public void refund() {
        if (getStatus() != TransactionStatus.DISPUTED) {
            throw new BusinessException(MSG_ONLY_DISPUTED_CAN_BE_REFUNDED);
        }
        setStatus(TransactionStatus.REFUNDED);
    }

}
