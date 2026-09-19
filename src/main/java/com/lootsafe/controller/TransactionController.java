package com.lootsafe.controller;

import com.lootsafe.dto.request.TransactionRequestDTO;
import com.lootsafe.dto.response.CredentialsResponseDTO;
import com.lootsafe.dto.response.DisputeResponseDTO;
import com.lootsafe.dto.response.TransactionResponseDTO;
import com.lootsafe.service.DigitalProductDeliveryService;
import com.lootsafe.service.DisputeService;
import com.lootsafe.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final DigitalProductDeliveryService digitalProductDeliveryService;
    private final DisputeService disputeService;

    @PostMapping(path = {"", "/initiate"})
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUYER')")
    public TransactionResponseDTO initiateTransaction(@RequestBody(required = false) TransactionRequestDTO request,
                                                      @RequestParam(name = "token", required = false) String tokenParam,
                                                      @AuthenticationPrincipal UUID currentUserId) {
        String token = (request != null && request.announcementToken() != null && !request.announcementToken().isBlank())
                ? request.announcementToken()
                : tokenParam;
        if (token == null || token.isBlank()) {
            throw new com.lootsafe.exception.BusinessException("O token do anúncio é obrigatório.");
        }
        return transactionService.initiateTransaction(token, currentUserId);
    }

    @PostMapping("/{id}/simulate-payment")
    public TransactionResponseDTO simulatePaymentApproval(@PathVariable UUID id,
                                                          @AuthenticationPrincipal UUID currentUserId) {
        return transactionService.simulatePaymentApproval(id, currentUserId);
    }

    @GetMapping("/me/purchases")
    public java.util.List<TransactionResponseDTO> getMyPurchases(@AuthenticationPrincipal UUID currentUserId) {
        return transactionService.getMyPurchases(currentUserId);
    }

    @GetMapping("/me/sales")
    public java.util.List<TransactionResponseDTO> getMySales(@AuthenticationPrincipal UUID currentUserId) {
        return transactionService.getMySales(currentUserId);
    }

    @GetMapping("/{id}")
    public TransactionResponseDTO getTransactionForUser(@PathVariable UUID id,
                                                     @AuthenticationPrincipal UUID currentUserId) {
        return transactionService.getTransactionForUser(id, currentUserId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/{id}/credentials")
    public CredentialsResponseDTO getTransactionCredentials(@PathVariable UUID id,
                                                            @AuthenticationPrincipal UUID currentUserId) {
        return digitalProductDeliveryService.deliverCredentials(id, currentUserId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping(path = {"/{id}/confirm", "/{id}/confirm-receipt"})
    public TransactionResponseDTO confirmReceipt(@PathVariable UUID id,
                                                 @AuthenticationPrincipal UUID currentUserId){
        return transactionService.confirmReceipt(id, currentUserId);
    }

    // ─── Dispute endpoints (acessados via /api/transactions/{id}/dispute) ────

    @PostMapping("/{id}/dispute")
    @ResponseStatus(HttpStatus.CREATED)
    public DisputeResponseDTO openDispute(@PathVariable UUID id,
                                          @RequestBody Map<String, String> body,
                                          @AuthenticationPrincipal UUID currentUserId) {
        String reason = body.getOrDefault("reason", "");
        return disputeService.openDispute(id, currentUserId, reason);
    }

    @GetMapping("/{id}/dispute")
    public DisputeResponseDTO getDisputeByTransaction(@PathVariable UUID id,
                                                      @AuthenticationPrincipal UUID currentUserId) {
        transactionService.getTransactionForUser(id, currentUserId);
        return disputeService.findByTransactionId(id);
    }
}