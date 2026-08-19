package com.lootsafe.controller;

import com.lootsafe.dto.request.TransactionRequestDTO;
import com.lootsafe.dto.response.CredentialsResponseDTO;
import com.lootsafe.dto.response.TransactionResponseDTO;
import com.lootsafe.service.DigitalProductDeliveryService;
import com.lootsafe.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final DigitalProductDeliveryService digitalProductDeliveryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUYER')")
    public TransactionResponseDTO initiateTransaction(@RequestBody @Valid TransactionRequestDTO request,
                                                      @AuthenticationPrincipal UUID currentUserId) {
        return transactionService.initiateTransaction(request.announcementToken(), currentUserId);
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
    @PostMapping("/{id}/confirm")
    public TransactionResponseDTO confirmReceipt(@PathVariable UUID id,
                                                 @AuthenticationPrincipal UUID currentUserId){

        return transactionService.confirmReceipt(id, currentUserId);

    }
}