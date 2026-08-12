package com.lootsafe.entity;


import com.lootsafe.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User extends AbstractAuditableEntity{

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String pixKey;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new HashSet<>();

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    private List<Announcement> announcements;

    @OneToMany(mappedBy = "buyer", fetch = FetchType.LAZY)
    private List<Transaction> purchases;

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    private List<Transaction> sales;

    @OneToMany(mappedBy = "initiatedBy", fetch = FetchType.LAZY)
    private List<DisputeChat> openedDisputes;

    public boolean hasRole(UserRole role) {
        return this.roles != null && this.roles.contains(role);
    }
}
