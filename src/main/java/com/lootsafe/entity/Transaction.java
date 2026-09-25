package com.lootsafe.entity;

import com.lootsafe.enums.PayoutStatus;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

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

    private static final String MSG_ONLY_APPROVED_CAN_BE_CONFIRMED =
            "A transação só pode ser confirmada quando aprovada.";

    private static final String MSG_INSPECTION_HOURS_MUST_BE_POSITIVE =
            "A quantidade de horas para inspeção deve ser maior que zero.";

    private static final String MSG_ONLY_APPROVED_AND_IN_INSPECTION_CAN_BE_AUTO_RELEASED =
            "Apenas transações aprovadas e em período de inspeção podem sofrer liberação automática.";

    private static final String MSG_INSPECTION_PERIOD_NOT_EXPIRED =
            "O período de inspeção desta transação ainda não expirou.";

    private static final String MSG_ONLY_DISPUTED_CAN_CANCEL_DISPUTE =
            "Apenas transações em disputa podem ter a disputa cancelada.";

    private static final String MSG_MAX_DISPUTE_ATTEMPTS_REACHED =
            "Você atingiu o limite máximo de 4 aberturas de disputa para esta transação.";

    private static final String MSG_PLATFORM_FEE_INVALID =
            "O valor da taxa da plataforma deve ser informado e não pode ser negativo.";

    private static final String MSG_TRANSACTION_AMOUNT_REQUIRED_FOR_FEES =
            "O valor total da transação deve estar definido para aplicar taxas.";

    private static final String MSG_PLATFORM_FEE_CANNOT_EXCEED_TOTAL =
            "A taxa da plataforma não pode ser superior ao valor total da transação.";

    private static final String MSG_INSPECTION_PERIOD_ALREADY_EXPIRED =
            "O período de inspeção já expirou. Não é mais possível abrir disputa para esta transação.";

    public static final int MAX_DISPUTE_ATTEMPTS = 4;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
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

    @Column(name = "dispute_attempts", nullable = false)
    private Integer disputeAttempts = 0;

    @Column(name = "inspection_time_hours")
    private Integer inspectionTimeHours;

    @Column(name = "inspection_expires_at")
    private Instant inspectionExpiresAt;

    @Column(name = "platform_fee", precision = 10, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "net_amount", precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_status", length = 30)
    private PayoutStatus payoutStatus = PayoutStatus.NOT_APPLICABLE;

    @Column(name = "payout_external_id")
    private String payoutExternalId;

    @Column(name = "payout_paid_at")
    private Instant payoutPaidAt;

    @Column(name = "payout_failure_reason", columnDefinition = "TEXT")
    private String payoutFailureReason;



    public boolean isPending() {
        return getStatus() == TransactionStatus.PENDING;
    }

    public boolean isApproved() {
        return getStatus() == TransactionStatus.APPROVED;
    }

    public void startInspectionWindow(int hours) {
        if (hours <= 0) {
            throw new BusinessException(MSG_INSPECTION_HOURS_MUST_BE_POSITIVE);
        }
        this.inspectionTimeHours = hours;
        this.inspectionExpiresAt = Instant.now().plus(Duration.ofHours(hours));
    }

    public boolean isInspectionExpired() {
        return this.inspectionExpiresAt != null && this.inspectionExpiresAt.isBefore(Instant.now());
    }

    public void pauseInspectionWindow() {

        inspectionExpiresAt = null;

    }


    public void markAsDisputed() {
        if (getStatus() == TransactionStatus.DISPUTED) {
            return;
        }
        if (getStatus() != TransactionStatus.PENDING
                && getStatus() != TransactionStatus.APPROVED) {
            throw new BusinessException(MSG_DISPUTE_NOT_ALLOWED_IN_STATE);
        }

        if (isInspectionExpired()) {
            throw new BusinessException(MSG_INSPECTION_PERIOD_ALREADY_EXPIRED);
        }

        if (this.disputeAttempts == null) {
            this.disputeAttempts = 0;
        }

        if (this.disputeAttempts >= MAX_DISPUTE_ATTEMPTS) {
            throw new BusinessException(MSG_MAX_DISPUTE_ATTEMPTS_REACHED);
        }

        this.disputeAttempts++;

        setStatus(TransactionStatus.DISPUTED);
    }


    public void cancelDispute() {
        if (getStatus() != TransactionStatus.DISPUTED) {
            throw new BusinessException(MSG_ONLY_DISPUTED_CAN_CANCEL_DISPUTE);
        }
        setStatus(TransactionStatus.APPROVED);
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



    public void confirmReceipt() {
        if (getStatus() == TransactionStatus.RELEASED) {
            return;
        }

        if (getStatus() != TransactionStatus.APPROVED) {
            throw new BusinessException(MSG_ONLY_APPROVED_CAN_BE_CONFIRMED);
        }

        setStatus(TransactionStatus.RELEASED);
        this.payoutStatus = PayoutStatus.PENDING;
    }


    public void applyFees(BigDecimal feeAmount) {
        if (feeAmount == null || feeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(MSG_PLATFORM_FEE_INVALID);
        }

        if (this.amount == null) {
            throw new BusinessException(MSG_TRANSACTION_AMOUNT_REQUIRED_FOR_FEES);
        }

        if (feeAmount.compareTo(this.amount) > 0) {
            throw new BusinessException(MSG_PLATFORM_FEE_CANNOT_EXCEED_TOTAL);
        }

        this.platformFee = feeAmount;
        this.netAmount = this.amount.subtract(feeAmount);
    }


    public void release() {
        if (getStatus() != TransactionStatus.DISPUTED) {
            throw new BusinessException(MSG_ONLY_DISPUTED_CAN_BE_RELEASED);
        }
        setStatus(TransactionStatus.RELEASED);
        this.payoutStatus = PayoutStatus.PENDING;
    }

    public void autoRelease() {
        if (getStatus() != TransactionStatus.APPROVED) {
            throw new BusinessException(MSG_ONLY_APPROVED_AND_IN_INSPECTION_CAN_BE_AUTO_RELEASED);
        }

        if (!isInspectionExpired()) {
            throw new BusinessException(MSG_INSPECTION_PERIOD_NOT_EXPIRED);
        }

        setStatus(TransactionStatus.RELEASED);
        this.payoutStatus = PayoutStatus.PENDING;
    }


    public void refund() {
        if (getStatus() != TransactionStatus.DISPUTED) {
            throw new BusinessException(MSG_ONLY_DISPUTED_CAN_BE_REFUNDED);
        }
        setStatus(TransactionStatus.REFUNDED);
    }

}
