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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;



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
            assertThrows(BusinessException.class, () -> transaction.applyFees(null));
        }

        @Test
        void applyFees_deveLancarExcecao_quandoTaxaForNegativa() {
            assertThrows(BusinessException.class,
                    () -> transaction.applyFees(new BigDecimal("-1.00")));
        }

        @Test
        void applyFees_deveLancarExcecao_quandoAmountForNull() {
            transaction.setAmount(null);

            assertThrows(BusinessException.class,
                    () -> transaction.applyFees(new BigDecimal("6.00")));
        }

        @Test
        void applyFees_deveLancarExcecao_quandoTaxaForMaiorQueAmount() {
            assertThrows(BusinessException.class,
                    () -> transaction.applyFees(new BigDecimal("110.00")));
        }

        @Test
        void applyFees_deveGravarPlatformFeeENetAmount_quandoEntradaValida() {
            transaction.applyFees(new BigDecimal("6.00"));

            assertEquals(0, new BigDecimal("6.00").compareTo(transaction.getPlatformFee()));
            assertEquals(0, new BigDecimal("94.00").compareTo(transaction.getNetAmount()));
        }
    }

    @Nested
    class autoRelease {

        private Transaction transaction;

        @BeforeEach
        void setUp() {

            transaction = new Transaction();
        }

        @Test
        void autoRelease_deveLancarExcecao_quandoStatusForDiferenteDeApproved() {

            transaction.setStatus(TransactionStatus.DISPUTED);

            assertThrows(BusinessException.class,
                    () -> transaction.autoRelease());

        }

        @Test
        void autoRelease_deveLancarExcecao_quandoOPeriodoDeInspecaoAindaNaoExpirou() {

            transaction.setStatus(TransactionStatus.APPROVED);
            transaction.setInspectionTimeHours(2);

            assertThrows(
                    BusinessException.class,
                    () -> transaction.autoRelease()
            );
        }

        @Test
        void autoRelease_deveLiberarTransacaoEMarcarPayoutComoPending_quandoInspecaoTiverExpirado() {

            transaction.setStatus(TransactionStatus.APPROVED);
            transaction.setInspectionExpiresAt(Instant.now().minus(Duration.ofHours(1)));

            transaction.autoRelease();

            assertEquals(TransactionStatus.RELEASED, transaction.getStatus());
            assertEquals(PayoutStatus.PENDING, transaction.getPayoutStatus());
        }
    }
}
