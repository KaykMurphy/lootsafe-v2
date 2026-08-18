package com.lootsafe.controller;

import com.lootsafe.dto.response.*;
import com.lootsafe.entity.Payment;
import com.lootsafe.enums.PaymentStatus;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.payment.service.PaymentService;
import com.lootsafe.service.AnnouncementService;
import com.lootsafe.service.DisputeService;
import com.lootsafe.service.TransactionService;
import com.lootsafe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final DisputeService disputeService;
    private final PaymentService paymentService;
    private final AnnouncementService announcementService;

    @GetMapping("/users")
    public List<UserResponseDTO> listUsers() {

        return userService.findAllUsers();

    }

    @GetMapping("/transactions")
    public List<TransactionResponseDTO> listTransactions(
            @RequestParam(required = false) TransactionStatus status
    ) {
        return transactionService.listTransactions(status);
    }

    @GetMapping("/disputes")
    public List<DisputeResponseDTO> listDisputes() {
        return disputeService.listDisputes();
    }

    @GetMapping("/payments")
    public List<PaymentResponseDTO> listPayment(
            @RequestParam(required = false) PaymentStatus status
    ) {

        return paymentService.listPayments(status);
    }

    @PostMapping("/transactions/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public TransactionResponseDTO cancelTransaction(@PathVariable UUID id) {
        return transactionService.cancelTransaction(id);
    }

    @PostMapping("/transactions/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public TransactionResponseDTO refundTransaction(@PathVariable UUID id) {
        return transactionService.refundTransaction(id);
    }

    @PostMapping("/payments/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelPayment(@PathVariable UUID id) {
        paymentService.cancelPayment(id);
        return ResponseEntity.noContent().build();
    }

}
