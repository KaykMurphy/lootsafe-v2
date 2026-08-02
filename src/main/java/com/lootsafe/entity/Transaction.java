package com.lootsafe.entity;

import com.lootsafe.enums.TransactionStatus;
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
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Table(name = "transactions")
public class Transaction extends AbstractAuditableEntity{

    private String mercadoPagoPaymentId;

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

    @OneToOne(mappedBy = "transaction", fetch = FetchType.LAZY,
    cascade = CascadeType.ALL, orphanRemoval = true)
    private DisputeChat dispute;


}
