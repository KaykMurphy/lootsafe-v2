package com.lootsafe.entity;

import com.lootsafe.enums.AnnouncementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
@Table(name = "announcements")
public class Announcement extends AbstractAuditableEntity {

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




}
