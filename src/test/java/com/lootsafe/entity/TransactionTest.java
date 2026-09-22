package com.lootsafe.entity;

import com.lootsafe.enums.PayoutStatus;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;


class TransactionTest {

    @Nested
    class ApplyFees {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction();
            transaction.setAmount(new BigDecimal("100.00"));
        }

        @Test
        void applyFees_deveLancarExcecao_quandoTaxaForNull() {
            assertThrows(
                    BusinessException.class,
                    () -> transaction.applyFees(null)
            );
        }

        @Test
        void applyFees_deveLancarExcecao_quandoTaxaForNegativa() {
            assertThrows(
                    BusinessException.class,
                    () -> transaction.applyFees(new BigDecimal("-1.00"))
            );
        }

        @Test
        void applyFees_deveLancarExcecao_quandoAmountForNull() {
            transaction.setAmount(null);

            assertThrows(
                    BusinessException.class,
                    () -> transaction.applyFees(new BigDecimal("6.00"))
            );
        }

        @Test
        void applyFees_deveLancarExcecao_quandoTaxaForMaiorQueAmount() {
            assertThrows(
                    BusinessException.class,
                    () -> transaction.applyFees(new BigDecimal("110.00"))
            );
        }

        @Test
        void applyFees_deveGravarPlatformFeeENetAmount_quandoEntradaValida() {
            transaction.applyFees(new BigDecimal("6.00"));

            assertEquals(
                    0,
                    new BigDecimal("6.00").compareTo(transaction.getPlatformFee())
            );

            assertEquals(
                    0,
                    new BigDecimal("94.00").compareTo(transaction.getNetAmount())
            );
        }
    }

    @Nested
    class AutoRelease {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction();
        }

        @Test
        void autoRelease_deveLancarExcecao_quandoStatusForDiferenteDeApproved() {
            transaction.setStatus(TransactionStatus.DISPUTED);

            assertThrows(
                    BusinessException.class,
                    () -> transaction.autoRelease()
            );
        }

        @Test
        void autoRelease_deveLancarExcecao_quandoOPeriodoDeInspecaoAindaNaoExpirou() {
            transaction.setStatus(TransactionStatus.APPROVED);
            transaction.setInspectionExpiresAt(
                    Instant.now().plus(Duration.ofHours(2))
            );

            assertThrows(
                    BusinessException.class,
                    () -> transaction.autoRelease()
            );
        }

        @Test
        void autoRelease_deveLiberarTransacaoEMarcarPayoutComoPending_quandoInspecaoTiverExpirado() {
            transaction.setStatus(TransactionStatus.APPROVED);
            transaction.setInspectionExpiresAt(
                    Instant.now().minus(Duration.ofHours(1))
            );

            transaction.autoRelease();

            assertEquals(
                    TransactionStatus.RELEASED,
                    transaction.getStatus()
            );

            assertEquals(
                    PayoutStatus.PENDING,
                    transaction.getPayoutStatus()
            );
        }
    }

    @Nested
    class ConfirmReceipt {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction();
        }

        @Test
        void confirmReceipt_naoDeveAlterarEstado_quandoJaEstiverReleased() {
            transaction.setStatus(TransactionStatus.RELEASED);

            transaction.confirmReceipt();

            assertEquals(
                    TransactionStatus.RELEASED,
                    transaction.getStatus()
            );
        }

        @Test
        void confirmReceipt_deveLancarExcecao_quandoStatusForDiferenteDeApproved() {
            transaction.setStatus(TransactionStatus.DISPUTED);

            assertThrows(
                    BusinessException.class,
                    () -> transaction.confirmReceipt()
            );

            assertEquals(
                    TransactionStatus.DISPUTED,
                    transaction.getStatus()
            );
        }

        @Test
        void confirmReceipt_deveLiberarTransacaoEMarcarPayoutComoPending_quandoStatusForApproved() {
            transaction.setStatus(TransactionStatus.APPROVED);

            transaction.confirmReceipt();

            assertEquals(
                    TransactionStatus.RELEASED,
                    transaction.getStatus()
            );

            assertEquals(
                    PayoutStatus.PENDING,
                    transaction.getPayoutStatus()
            );
        }
    }

    @Nested
    class StartInspectionWindow {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction();
        }

        @Test
        void startInspectionWindow_deveLancarExcecao_quandoHorasForemZero() {
            assertThrows(
                    BusinessException.class,
                    () -> transaction.startInspectionWindow(0)
            );
        }

        @Test
        void startInspectionWindow_deveLancarExcecao_quandoHorasForemNegativas() {
            assertThrows(
                    BusinessException.class,
                    () -> transaction.startInspectionWindow(-5)
            );
        }

        @Test
        void startInspectionWindow_deveDefinirInspectionTimeHoursEInspectionExpiresAt_quandoHorasForemValidas() {
            Instant beforeCall = Instant.now();

            transaction.startInspectionWindow(48);

            assertEquals(48, transaction.getInspectionTimeHours());
            assertNotNull(transaction.getInspectionExpiresAt());
            assertTrue(transaction.getInspectionExpiresAt().isAfter(beforeCall.plus(Duration.ofHours(47))));
        }
    }

    @Nested
    class IsInspectionExpired {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction();
        }

        @Test
        void isInspectionExpired_deveRetornarFalse_quandoInspectionExpiresAtForNull() {
            transaction.setInspectionExpiresAt(null);

            assertFalse(transaction.isInspectionExpired());
        }

        @Test
        void isInspectionExpired_deveRetornarFalse_quandoInspectionExpiresAtEstiverNoFuturo() {
            transaction.setInspectionExpiresAt(Instant.now().plus(Duration.ofHours(2)));

            assertFalse(transaction.isInspectionExpired());
        }

        @Test
        void isInspectionExpired_deveRetornarTrue_quandoInspectionExpiresAtEstiverNoPassado() {
            transaction.setInspectionExpiresAt(Instant.now().minus(Duration.ofHours(1)));

            assertTrue(transaction.isInspectionExpired());
        }
    }

    @Nested
    class PauseInspectionWindow {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction();
        }

        @Test
        void pauseInspectionWindow_deveDefinirInspectionExpiresAtComoNull() {
            transaction.setInspectionExpiresAt(Instant.now().plus(Duration.ofHours(12)));

            transaction.pauseInspectionWindow();

            assertNull(transaction.getInspectionExpiresAt());
        }
    }

    @Nested
    class Approve {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction();
        }

        @Test
        void approve_deveAlterarStatusParaApproved_quandoStatusForPending() {
            transaction.setStatus(TransactionStatus.PENDING);

            transaction.approve();

            assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        }

        @Test
        void approve_naoDeveAlterarEstado_quandoJaEstiverApproved() {
            transaction.setStatus(TransactionStatus.APPROVED);

            transaction.approve();

            assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        }

        @Test
        void approve_deveLancarExcecao_quandoStatusForDiferenteDePendingEApproved() {
            transaction.setStatus(TransactionStatus.DISPUTED);

            assertThrows(
                    BusinessException.class,
                    () -> transaction.approve()
            );
        }
    }

    @Nested
    class MarkAsDisputed {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction();
        }

        @Test
        void markAsDisputed_naoDeveAlterarEstado_quandoJaEstiverDisputed() {
            transaction.setStatus(TransactionStatus.DISPUTED);

            transaction.markAsDisputed();

            assertEquals(TransactionStatus.DISPUTED, transaction.getStatus());
        }

        @Test
        void markAsDisputed_deveAlterarStatusParaDisputed_quandoStatusForPending() {
            transaction.setStatus(TransactionStatus.PENDING);

            transaction.markAsDisputed();

            assertEquals(TransactionStatus.DISPUTED, transaction.getStatus());
        }

        @Test
        void markAsDisputed_deveAlterarStatusParaDisputed_quandoStatusForApproved() {
            transaction.setStatus(TransactionStatus.APPROVED);

            transaction.markAsDisputed();

            assertEquals(TransactionStatus.DISPUTED, transaction.getStatus());
        }

        @Test
        void markAsDisputed_deveLancarExcecao_quandoStatusNaoPermitirDisputa() {
            transaction.setStatus(TransactionStatus.RELEASED);

            assertThrows(
                    BusinessException.class,
                    () -> transaction.markAsDisputed()
            );
        }

        @Test
        void markAsDisputed_deveIncrementarTentativas_quandoEntradaValida() {
            transaction.setStatus(TransactionStatus.APPROVED);

            transaction.markAsDisputed();

            assertEquals(TransactionStatus.DISPUTED, transaction.getStatus());
            assertEquals(1, transaction.getDisputeAttempts());
        }

        @Test
        void markAsDisputed_deveLancarExcecao_quandoAtingirLimiteMaximoDeTentativas() {
            transaction.setStatus(TransactionStatus.APPROVED);
            transaction.setDisputeAttempts(4);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> transaction.markAsDisputed()
            );

            assertTrue(ex.getMessage().contains("limite máximo"));
        }
    }

    @Nested
    class CancelDispute {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction();
        }

        @Test
        void cancelDispute_deveAlterarStatusParaApproved_quandoEstiverDisputed() {
            transaction.setStatus(TransactionStatus.DISPUTED);

            transaction.cancelDispute();

            assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        }

        @Test
        void cancelDispute_deveLancarExcecao_quandoNaoEstiverDisputed() {
            transaction.setStatus(TransactionStatus.APPROVED);

            assertThrows(
                    BusinessException.class,
                    () -> transaction.cancelDispute()
            );
        }
    }

    @Nested
    class Release {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction();
        }

        @Test
        void release_deveLancarExcecao_quandoStatusForDiferenteDeDisputed() {
            transaction.setStatus(TransactionStatus.APPROVED);

            assertThrows(
                    BusinessException.class,
                    () -> transaction.release()
            );
        }

        @Test
        void release_deveAlterarStatusParaReleasedEMarcarPayoutComoPending_quandoStatusForDisputed() {
            transaction.setStatus(TransactionStatus.DISPUTED);

            transaction.release();

            assertEquals(TransactionStatus.RELEASED, transaction.getStatus());
            assertEquals(PayoutStatus.PENDING, transaction.getPayoutStatus());
        }
    }

    @Nested
    class Refund {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction();
        }

        @Test
        void refund_deveLancarExcecao_quandoStatusForDiferenteDeDisputed() {
            transaction.setStatus(TransactionStatus.PENDING);

            assertThrows(
                    BusinessException.class,
                    () -> transaction.refund()
            );
        }

        @Test
        void refund_deveAlterarStatusParaRefunded_quandoStatusForDisputed() {
            transaction.setStatus(TransactionStatus.DISPUTED);

            transaction.refund();

            assertEquals(TransactionStatus.REFUNDED, transaction.getStatus());
        }
    }

    @Nested
    class StatusCheckers {

        private Transaction transaction;

        @BeforeEach
        void setUp() {
            transaction = new Transaction();
        }

        @Test
        void isPending_deveRetornarTrue_quandoStatusForPending() {
            transaction.setStatus(TransactionStatus.PENDING);

            assertTrue(transaction.isPending());
        }

        @Test
        void isPending_deveRetornarFalse_quandoStatusForDiferenteDePending() {
            transaction.setStatus(TransactionStatus.APPROVED);

            assertFalse(transaction.isPending());
        }

        @Test
        void isApproved_deveRetornarTrue_quandoStatusForApproved() {
            transaction.setStatus(TransactionStatus.APPROVED);

            assertTrue(transaction.isApproved());
        }

        @Test
        void isApproved_deveRetornarFalse_quandoStatusForDiferenteDeApproved() {
            transaction.setStatus(TransactionStatus.PENDING);

            assertFalse(transaction.isApproved());
        }
    }
}