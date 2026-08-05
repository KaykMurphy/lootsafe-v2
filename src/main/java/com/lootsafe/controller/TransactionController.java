package com.lootsafe.controller;

import com.lootsafe.dto.request.TransactionRequestDTO;
import com.lootsafe.dto.response.TransactionResponseDTO;
import com.lootsafe.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponseDTO initiateTransaction(
            @RequestHeader("X-User-Id") UUID buyerId,
            @RequestBody @Valid TransactionRequestDTO request) {
        return transactionService.initiateTransaction(request.announcementToken(), buyerId);
    }

    @GetMapping("/{id}")
    public TransactionResponseDTO getTransactionById(@PathVariable UUID id) {
        return transactionService.getTransactionById(id);
    }
}