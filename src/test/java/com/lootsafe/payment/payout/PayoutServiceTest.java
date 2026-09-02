package com.lootsafe.payment.payout;

import com.lootsafe.entity.Announcement;
import com.lootsafe.entity.Transaction;
import com.lootsafe.entity.User;
import com.lootsafe.enums.PayoutStatus;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PayoutClient payoutClient;

    @InjectMocks
    private PayoutService payoutService;

    private UUID transactionId;
    private Transaction transaction;
    private Announcement announcement;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();

        User seller = new User();
        seller.setId(UUID.randomUUID());
        seller.setEmail("vendedor@lootsafe.com");

        announcement = new Announcement();
        announcement.setId(UUID.randomUUID());
        announcement.setSeller(seller);
        announcement.setPixKey("vendedor-pix-chave@banco.com");

        transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAnnouncement(announcement);
        transaction.setStatus(TransactionStatus.RELEASED);
        transaction.setPayoutStatus(PayoutStatus.PENDING);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setNetAmount(new BigDecimal("95.00"));
        transaction.setPlatformFee(new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("Deve processar o payout com sucesso quando a transação for liberada")
    void processPayout_Success() {
        Instant now = Instant.now();
        PayoutResult successResult = new PayoutResult(
                "ext-tx-12345",
                PayoutStatus.PAID,
                now,
                "{\"status\":\"PAID\"}",
                null
        );

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(payoutClient.transferPix(eq("vendedor-pix-chave@banco.com"), eq(new BigDecimal("95.00")), any(), any()))
                .thenReturn(successResult);

        payoutService.processPayout(transactionId);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());

        Transaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getPayoutStatus()).isEqualTo(PayoutStatus.PAID);
        assertThat(savedTx.getPayoutExternalId()).isEqualTo("ext-tx-12345");
        assertThat(savedTx.getPayoutPaidAt()).isEqualTo(now);
        assertThat(savedTx.getPayoutFailureReason()).isNull();
    }

    @Test
    @DisplayName("Deve utilizar o amount original se netAmount for nulo")
    void processPayout_FallbackToAmount_WhenNetAmountIsNull() {
        transaction.setNetAmount(null);
        Instant now = Instant.now();
        PayoutResult successResult = new PayoutResult("ext-1", PayoutStatus.PAID, now, "{}", null);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(payoutClient.transferPix(eq("vendedor-pix-chave@banco.com"), eq(new BigDecimal("100.00")), any(), any()))
                .thenReturn(successResult);

        payoutService.processPayout(transactionId);

        verify(payoutClient).transferPix(eq("vendedor-pix-chave@banco.com"), eq(new BigDecimal("100.00")), any(), any());
        verify(transactionRepository).save(transaction);
    }

    @Test
    @DisplayName("Deve ignorar chamada com transactionId nulo")
    void processPayout_NullId_Ignored() {
        payoutService.processPayout(null);

        verifyNoInteractions(transactionRepository);
        verifyNoInteractions(payoutClient);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando transação não existir")
    void processPayout_NotFound_ThrowsException() {
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payoutService.processPayout(transactionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transação não encontrada.");
    }

    @Test
    @DisplayName("Deve ignorar transação que não esteja com status RELEASED")
    void processPayout_NotReleased_Ignored() {
        transaction.setStatus(TransactionStatus.APPROVED);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        payoutService.processPayout(transactionId);

        verifyNoInteractions(payoutClient);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve ignorar transação que já foi paga (PAID)")
    void processPayout_AlreadyPaid_Ignored() {
        transaction.setPayoutStatus(PayoutStatus.PAID);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        payoutService.processPayout(transactionId);

        verifyNoInteractions(payoutClient);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve marcar como FAILED quando a chave Pix do vendedor estiver ausente")
    void processPayout_MissingPixKey_MarksFailed() {
        announcement.setPixKey(null);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        payoutService.processPayout(transactionId);

        verifyNoInteractions(payoutClient);
        verify(transactionRepository).save(transaction);
        assertThat(transaction.getPayoutStatus()).isEqualTo(PayoutStatus.FAILED);
        assertThat(transaction.getPayoutFailureReason()).isEqualTo("Chave Pix do vendedor não cadastrada no anúncio.");
    }

    @Test
    @DisplayName("Deve atualizar status para FAILED quando o cliente de payout retornar falha")
    void processPayout_ClientReturnsFailed() {
        PayoutResult failResult = new PayoutResult(
                null,
                PayoutStatus.FAILED,
                Instant.now(),
                "{\"error\":\"Chave Pix inexistente\"}",
                "Chave Pix inexistente"
        );

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(payoutClient.transferPix(any(), any(), any(), any())).thenReturn(failResult);

        payoutService.processPayout(transactionId);

        verify(transactionRepository).save(transaction);
        assertThat(transaction.getPayoutStatus()).isEqualTo(PayoutStatus.FAILED);
        assertThat(transaction.getPayoutFailureReason()).isEqualTo("Chave Pix inexistente");
    }

    @Test
    @DisplayName("Deve atualizar status para PROCESSING quando o cliente de payout retornar em processamento")
    void processPayout_ClientReturnsProcessing() {
        PayoutResult processingResult = new PayoutResult(
                "ext-proc-999",
                PayoutStatus.PROCESSING,
                Instant.now(),
                "{}",
                null
        );

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(payoutClient.transferPix(any(), any(), any(), any())).thenReturn(processingResult);

        payoutService.processPayout(transactionId);

        verify(transactionRepository).save(transaction);
        assertThat(transaction.getPayoutStatus()).isEqualTo(PayoutStatus.PROCESSING);
        assertThat(transaction.getPayoutExternalId()).isEqualTo("ext-proc-999");
    }

    @Test
    @DisplayName("Deve capturar exceção de rede/timeout e marcar payout como FAILED sem quebrar execução")
    void processPayout_ClientThrowsException_HandledGracefully() {
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(payoutClient.transferPix(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Timeout de conexão com o banco emissor"));

        payoutService.processPayout(transactionId);

        verify(transactionRepository).save(transaction);
        assertThat(transaction.getPayoutStatus()).isEqualTo(PayoutStatus.FAILED);
        assertThat(transaction.getPayoutFailureReason()).isEqualTo("Timeout de conexão com o banco emissor");
    }
}
