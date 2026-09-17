package com.lootsafe.entity;

import com.lootsafe.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Nested
    class HasRole {

        private User user;

        @BeforeEach
        void setUp() {
            user = new User();
        }

        @Test
        void hasRole_deveRetornarTrue_quandoUsuarioPossuirARole() {
            user.setRoles(new HashSet<>(Set.of(UserRole.BUYER)));

            assertTrue(user.hasRole(UserRole.BUYER));
        }

        @Test
        void hasRole_deveRetornarFalse_quandoUsuarioNaoPossuirARole() {
            user.setRoles(new HashSet<>(Set.of(UserRole.BUYER)));

            assertFalse(user.hasRole(UserRole.ADMIN));
        }

        @Test
        void hasRole_deveRetornarFalse_quandoRolesForNull() {
            user.setRoles(null);

            assertFalse(user.hasRole(UserRole.BUYER));
        }

        @Test
        void hasRole_deveRetornarFalse_quandoRoleConsultadaForNull() {
            user.setRoles(new HashSet<>(Set.of(UserRole.BUYER)));

            assertFalse(user.hasRole(null));
        }

        @Test
        void hasRole_deveRetornarTrue_quandoUsuarioPossuirMultiplasRolesEAConsultadaEstiverPresente() {
            user.setRoles(new HashSet<>(Set.of(UserRole.BUYER, UserRole.SELLER, UserRole.ADMIN)));

            assertTrue(user.hasRole(UserRole.SELLER));
        }
    }
}
