package com.lootsafe.entity;

import com.lootsafe.enums.ProposalStatus;
import com.lootsafe.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InspectionProposalTest {

    @Nested
    class IsPending {

        private InspectionProposal proposal;

        @BeforeEach
        void setUp() {
            proposal = new InspectionProposal();
        }

        @Test
        void isPending_deveRetornarTrue_quandoStatusForPending() {
            proposal.setStatus(ProposalStatus.PENDING);

            assertTrue(proposal.isPending());
        }

        @Test
        void isPending_deveRetornarFalse_quandoStatusForAccepted() {
            proposal.setStatus(ProposalStatus.ACCEPTED);

            assertFalse(proposal.isPending());
        }

        @Test
        void isPending_deveRetornarFalse_quandoStatusForRejected() {
            proposal.setStatus(ProposalStatus.REJECTED);

            assertFalse(proposal.isPending());
        }

        @Test
        void isPending_deveRetornarFalse_quandoStatusForExpired() {
            proposal.setStatus(ProposalStatus.EXPIRED);

            assertFalse(proposal.isPending());
        }
    }

    @Nested
    class Accept {

        private InspectionProposal proposal;

        @BeforeEach
        void setUp() {
            proposal = new InspectionProposal();
            proposal.setStatus(ProposalStatus.PENDING);
        }

        @Test
        void accept_deveLancarExcecao_quandoStatusNaoForPending() {
            proposal.setStatus(ProposalStatus.REJECTED);

            assertThrows(
                    BusinessException.class,
                    () -> proposal.accept("valid-token")
            );
        }

        @Test
        void accept_deveLancarExcecao_quandoTokenForNull() {
            assertThrows(
                    BusinessException.class,
                    () -> proposal.accept(null)
            );
        }

        @Test
        void accept_deveLancarExcecao_quandoTokenForVazioOuEmBranco() {
            assertThrows(
                    BusinessException.class,
                    () -> proposal.accept("   ")
            );
        }

        @Test
        void accept_deveAlterarStatusParaAcceptedEDefinirAuthorizationToken_quandoEntradaValida() {
            proposal.accept("auth-token-123");

            assertEquals(ProposalStatus.ACCEPTED, proposal.getStatus());
            assertEquals("auth-token-123", proposal.getAuthorizationToken());
        }
    }

    @Nested
    class Reject {

        private InspectionProposal proposal;

        @BeforeEach
        void setUp() {
            proposal = new InspectionProposal();
        }

        @Test
        void reject_deveLancarExcecao_quandoStatusNaoForPending() {
            proposal.setStatus(ProposalStatus.ACCEPTED);

            assertThrows(
                    BusinessException.class,
                    () -> proposal.reject()
            );
        }

        @Test
        void reject_deveAlterarStatusParaRejected_quandoStatusForPending() {
            proposal.setStatus(ProposalStatus.PENDING);

            proposal.reject();

            assertEquals(ProposalStatus.REJECTED, proposal.getStatus());
        }
    }

    @Nested
    class Expire {

        private InspectionProposal proposal;

        @BeforeEach
        void setUp() {
            proposal = new InspectionProposal();
        }

        @Test
        void expire_deveAlterarStatusParaExpired_quandoStatusForPending() {
            proposal.setStatus(ProposalStatus.PENDING);

            proposal.expire();

            assertEquals(ProposalStatus.EXPIRED, proposal.getStatus());
        }

        @Test
        void expire_naoDeveAlterarStatus_quandoStatusNaoForPending() {
            proposal.setStatus(ProposalStatus.ACCEPTED);

            proposal.expire();

            assertEquals(ProposalStatus.ACCEPTED, proposal.getStatus());
        }
    }
}
