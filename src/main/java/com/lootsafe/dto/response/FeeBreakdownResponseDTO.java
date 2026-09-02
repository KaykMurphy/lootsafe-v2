package com.lootsafe.dto.response;

import java.math.BigDecimal;

public record FeeBreakdownResponseDTO(

        BigDecimal grossAmount,
        BigDecimal platformFee,
        BigDecimal netAmount

) {
}
