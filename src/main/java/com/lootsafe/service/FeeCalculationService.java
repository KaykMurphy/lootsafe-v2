package com.lootsafe.service;

import com.lootsafe.config.PlatformProperties;
import com.lootsafe.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@RequiredArgsConstructor
@Service
public class FeeCalculationService {

    private final PlatformProperties platformProperties;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int FINANCIAL_SCALE = 2;
    private static final RoundingMode FINANCIAL_ROUNDING = RoundingMode.HALF_EVEN;

    private static final String MSG_GROSS_AMOUNT_INVALID =
            "O valor bruto da transação deve ser informado e maior ou igual a zero.";

    private static final String MSG_AMOUNTS_CANNOT_BE_NULL =
            "O valor bruto e a taxa da plataforma não podem ser nulos.";
    private static final String MSG_FEE_CANNOT_EXCEED_GROSS =
            "A taxa da plataforma não pode ser maior que o valor total da transação.";

    public BigDecimal calculatePlatformFee(BigDecimal grossAmount) {
        if (grossAmount == null || grossAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(MSG_GROSS_AMOUNT_INVALID);
        }

        BigDecimal percentageFee = grossAmount
                .multiply(platformProperties.getPercentage())
                .divide(ONE_HUNDRED, 4, FINANCIAL_ROUNDING);

        BigDecimal totalFee = percentageFee
                .add(platformProperties.getFeeFixed())
                .setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);

        log.debug("Taxa da plataforma calculada: bruto={}, percentual={}%, fixa={}, totalTaxa={}",
                grossAmount, platformProperties.getPercentage(), platformProperties.getFeeFixed(), totalFee);

        return totalFee;
    }

    public BigDecimal calculateNetAmount(BigDecimal grossAmount, BigDecimal platformFee) {
        if (grossAmount == null || platformFee == null) {
            throw new BusinessException(MSG_AMOUNTS_CANNOT_BE_NULL);
        }

        if (platformFee.compareTo(grossAmount) > 0) {
            throw new BusinessException(MSG_FEE_CANNOT_EXCEED_GROSS);
        }

        BigDecimal netAmount = grossAmount
                .subtract(platformFee)
                .setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);

        log.debug("Valor líquido apurado: bruto={}, taxa={}, liquido={}",
                grossAmount, platformFee, netAmount);

        return netAmount;
    }



}
