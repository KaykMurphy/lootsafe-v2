package com.lootsafe.controller;

import com.lootsafe.dto.response.PaymentResponseDTO;
import com.lootsafe.entity.Payment;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.mapper.PaymentMapper;
import com.lootsafe.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @GetMapping("/{id}")
    public PaymentResponseDTO getPayment(@PathVariable UUID id) {
        Payment payment = paymentRepository.findById(id)
                .or(() -> paymentRepository.findByTransactionId(id).stream().findFirst())
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado."));
        return paymentMapper.toResponse(payment);
    }
}
