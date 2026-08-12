package com.lootsafe.entity;

import com.lootsafe.enums.AnnouncementStatus;
import com.lootsafe.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "announcements")
public class Announcement extends AbstractAuditableEntity {

    private static final String MSG_NOT_AVAILABLE_FOR_PURCHASE =
            "Este anúncio não está disponível para compra.";
    private static final String MSG_ONLY_RESERVED_CAN_BE_SOLD =
            "O anúncio só pode ser marcado como vendido quando está reservado.";
    private static final String MSG_ONLY_DRAFT_OR_ACTIVE_CAN_BE_CANCELLED =
            "Apenas anúncios ativos ou em rascunho podem ser cancelados.";

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String credentialsEncrypted;

    @Column(nullable = false)
    private String notes;

    @Column(nullable = false)
    private String pixKey;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "seller_id", nullable = false
    )
    private User seller;

    @OneToOne(mappedBy = "announcement", fetch = FetchType.LAZY,
    cascade = CascadeType.ALL, orphanRemoval = true)
    private Transaction transaction;

    public void reserve() {
        if (getStatus() != AnnouncementStatus.ACTIVE) {
            throw new BusinessException(MSG_NOT_AVAILABLE_FOR_PURCHASE);
        }
        setStatus(AnnouncementStatus.RESERVED);
    }

    public void markAsSold() {
        if (getStatus() == AnnouncementStatus.SOLD) {
            return;
        }
        if (getStatus() != AnnouncementStatus.RESERVED) {
            throw new BusinessException(MSG_ONLY_RESERVED_CAN_BE_SOLD);
        }
        setStatus(AnnouncementStatus.SOLD);
    }

    public void cancel() {
        if (!isEditable()) {
            throw new BusinessException(MSG_ONLY_DRAFT_OR_ACTIVE_CAN_BE_CANCELLED);
        }
        setStatus(AnnouncementStatus.CANCELLED);
    }

    public boolean isEditable() {
        return getStatus() == AnnouncementStatus.DRAFT
                || getStatus() == AnnouncementStatus.ACTIVE;
    }


}
