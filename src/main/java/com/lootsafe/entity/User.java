package com.lootsafe.entity;


import com.lootsafe.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
@Table(name = "users")
public class User extends AbstractAuditableEntity{

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String pixKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    private List<Announcement> announcements;

    @OneToMany(mappedBy = "buyer", fetch = FetchType.LAZY)
    private List<Transaction> purchases;

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    private List<Transaction> sales;

    @OneToMany(mappedBy = "initiatedBy", fetch = FetchType.LAZY)
    private List<DisputeChat> openedDisputes;


}
