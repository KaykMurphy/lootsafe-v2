package com.lootsafe.service;

import com.lootsafe.dto.response.PaymentResponseDTO;
import com.lootsafe.dto.response.TransactionResponseDTO;
import com.lootsafe.entity.Announcement;
import com.lootsafe.entity.Transaction;
import com.lootsafe.entity.User;
import com.lootsafe.enums.AnnouncementStatus;
import com.lootsafe.enums.PaymentStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.mapper.TransactionMapper;
import com.lootsafe.payment.service.PaymentService;
import com.lootsafe.repository.AnnouncementRepository;
import com.lootsafe.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private UserService userService;
    @Mock private PaymentService paymentService;
    @Mock private FeeCalculationService feeCalculationService;
    @Mock private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private Announcement announcement;
    private User user;

    private TransactionResponseDTO responseDTO() {
        return new TransactionResponseDTO(
                UUID.randomUUID(), null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null
        );
    }

    @Nested
    class InitiateTransaction {

        @BeforeEach
        void setUp() {
            announcement = new Announcement();
            user = new User();
        }

        @Test
        void deveLancarExcecao_quandoTokenNaoForEncontrado() {

            UUID buyerId = UUID.randomUUID();
            String tokenInexistente = "token-nao-existe";

            when(announcementRepository.findByTokenWithLock(tokenInexistente))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> transactionService.initiateTransaction(tokenInexistente, buyerId));
        }

        @Test
        void deveLancarExcecao_quandoOVendedorTentaComprarOProprioAnuncio() {

            UUID sameId = UUID.randomUUID();
            String tokenExistente = "token-existe";

            user.setId(sameId);
            announcement.setSeller(user);

            when(announcementRepository.findByTokenWithLock(tokenExistente))
                    .thenReturn(Optional.of(announcement));

            assertThrows(BusinessException.class,
                    () -> transactionService.initiateTransaction(tokenExistente, sameId));
        }

        @Test
        void deveCriarTransacaoComStatusPending_quandoDadosForemValidos() {

            UUID sellerId = UUID.randomUUID();
            UUID buyerId = UUID.randomUUID();
            String tokenExistente = "token-existe";

            user.setId(sellerId);
            announcement.setSeller(user);
            announcement.setPrice(new BigDecimal("100.00"));
            announcement.setStatus(AnnouncementStatus.ACTIVE);

            User buyer = new User();
            buyer.setId(buyerId);

            Transaction saved = new Transaction();
            saved.setId(UUID.randomUUID());

            PaymentResponseDTO paymentDTO = new PaymentResponseDTO(
                    UUID.randomUUID(),
                    saved.getId(),
                    PaymentStatus.PENDING,
                    new BigDecimal("100.00"),
                    "pix-code",
                    "base64",
                    Instant.now().plus(Duration.ofHours(24))
            );

            when(announcementRepository.findByTokenWithLock(tokenExistente))
                    .thenReturn(Optional.of(announcement));

            when(userService.findEntityById(buyerId)).thenReturn(buyer);

            when(feeCalculationService.calculatePlatformFee(new BigDecimal("100.00")))
                    .thenReturn(new BigDecimal("6.00"));

            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(saved);

            when(paymentService.createPayment(saved.getId()))
                    .thenReturn(paymentDTO);

            when(transactionMapper.toResponse(saved)).thenReturn(responseDTO());

            transactionService.initiateTransaction(tokenExistente, buyerId);

            verify(transactionRepository).save(any(Transaction.class));
            verify(paymentService).createPayment(saved.getId());
            assertEquals(AnnouncementStatus.RESERVED, announcement.getStatus());
        }
    }
}