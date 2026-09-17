package com.lootsafe.entity;

import com.lootsafe.enums.AnnouncementStatus;
import com.lootsafe.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnnouncementTest {

    @Nested
    class ValidateInspectionTimeLimits {

        private Announcement announcement;

        @BeforeEach
        void setUp() {
            announcement = new Announcement();
        }

        @Test
        void validateInspectionTimeLimits_deveLancarExcecao_quandoHorasForemMenoresQueMinimo() {
            announcement.setInspectionTimeHours(1);

            assertThrows(
                    BusinessException.class,
                    () -> announcement.validateInspectionTimeLimits(2, 48)
            );
        }

        @Test
        void validateInspectionTimeLimits_deveLancarExcecao_quandoHorasForemMaioresQueMaximo() {
            announcement.setInspectionTimeHours(50);

            assertThrows(
                    BusinessException.class,
                    () -> announcement.validateInspectionTimeLimits(2, 48)
            );
        }

        @Test
        void validateInspectionTimeLimits_devePassarSemExcecao_quandoHorasForemIguaisAoMinimo() {
            announcement.setInspectionTimeHours(2);

            assertDoesNotThrow(() -> announcement.validateInspectionTimeLimits(2, 48));
        }

        @Test
        void validateInspectionTimeLimits_devePassarSemExcecao_quandoHorasForemIguaisAoMaximo() {
            announcement.setInspectionTimeHours(48);

            assertDoesNotThrow(() -> announcement.validateInspectionTimeLimits(2, 48));
        }

        @Test
        void validateInspectionTimeLimits_devePassarSemExcecao_quandoHorasEstiveremDentroDosLimites() {
            announcement.setInspectionTimeHours(24);

            assertDoesNotThrow(() -> announcement.validateInspectionTimeLimits(2, 48));
        }
    }

    @Nested
    class Reserve {

        private Announcement announcement;

        @BeforeEach
        void setUp() {
            announcement = new Announcement();
        }

        @Test
        void reserve_deveAlterarStatusParaReserved_quandoStatusForActive() {
            announcement.setStatus(AnnouncementStatus.ACTIVE);

            announcement.reserve();

            assertEquals(AnnouncementStatus.RESERVED, announcement.getStatus());
        }

        @Test
        void reserve_deveLancarExcecao_quandoStatusForDiferenteDeActive() {
            announcement.setStatus(AnnouncementStatus.DRAFT);

            assertThrows(
                    BusinessException.class,
                    () -> announcement.reserve()
            );
        }

        @Test
        void reserve_deveLancarExcecao_quandoStatusForSold() {
            announcement.setStatus(AnnouncementStatus.SOLD);

            assertThrows(
                    BusinessException.class,
                    () -> announcement.reserve()
            );
        }
    }

    @Nested
    class MarkAsSold {

        private Announcement announcement;

        @BeforeEach
        void setUp() {
            announcement = new Announcement();
        }

        @Test
        void markAsSold_deveAlterarStatusParaSold_quandoStatusForReserved() {
            announcement.setStatus(AnnouncementStatus.RESERVED);

            announcement.markAsSold();

            assertEquals(AnnouncementStatus.SOLD, announcement.getStatus());
        }

        @Test
        void markAsSold_naoDeveAlterarEstado_quandoJaEstiverSold() {
            announcement.setStatus(AnnouncementStatus.SOLD);

            announcement.markAsSold();

            assertEquals(AnnouncementStatus.SOLD, announcement.getStatus());
        }

        @Test
        void markAsSold_deveLancarExcecao_quandoStatusForDiferenteDeReservedESold() {
            announcement.setStatus(AnnouncementStatus.ACTIVE);

            assertThrows(
                    BusinessException.class,
                    () -> announcement.markAsSold()
            );
        }
    }

    @Nested
    class Cancel {

        private Announcement announcement;

        @BeforeEach
        void setUp() {
            announcement = new Announcement();
        }

        @Test
        void cancel_deveAlterarStatusParaCancelled_quandoStatusForDraft() {
            announcement.setStatus(AnnouncementStatus.DRAFT);

            announcement.cancel();

            assertEquals(AnnouncementStatus.CANCELLED, announcement.getStatus());
        }

        @Test
        void cancel_deveAlterarStatusParaCancelled_quandoStatusForActive() {
            announcement.setStatus(AnnouncementStatus.ACTIVE);

            announcement.cancel();

            assertEquals(AnnouncementStatus.CANCELLED, announcement.getStatus());
        }

        @Test
        void cancel_deveLancarExcecao_quandoStatusNaoForEditavel() {
            announcement.setStatus(AnnouncementStatus.RESERVED);

            assertThrows(
                    BusinessException.class,
                    () -> announcement.cancel()
            );
        }

        @Test
        void cancel_deveLancarExcecao_quandoStatusJaForCancelled() {
            announcement.setStatus(AnnouncementStatus.CANCELLED);

            assertThrows(
                    BusinessException.class,
                    () -> announcement.cancel()
            );
        }
    }

    @Nested
    class IsEditable {

        private Announcement announcement;

        @BeforeEach
        void setUp() {
            announcement = new Announcement();
        }

        @Test
        void isEditable_deveRetornarTrue_quandoStatusForDraft() {
            announcement.setStatus(AnnouncementStatus.DRAFT);

            assertTrue(announcement.isEditable());
        }

        @Test
        void isEditable_deveRetornarTrue_quandoStatusForActive() {
            announcement.setStatus(AnnouncementStatus.ACTIVE);

            assertTrue(announcement.isEditable());
        }

        @Test
        void isEditable_deveRetornarFalse_quandoStatusForReserved() {
            announcement.setStatus(AnnouncementStatus.RESERVED);

            assertFalse(announcement.isEditable());
        }

        @Test
        void isEditable_deveRetornarFalse_quandoStatusForSold() {
            announcement.setStatus(AnnouncementStatus.SOLD);

            assertFalse(announcement.isEditable());
        }

        @Test
        void isEditable_deveRetornarFalse_quandoStatusForCancelled() {
            announcement.setStatus(AnnouncementStatus.CANCELLED);

            assertFalse(announcement.isEditable());
        }
    }
}
