package com.lootsafe.payment.payout;

import com.lootsafe.enums.PayoutStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnMissingBean(name = "mercadopagoPayoutClient")
public class MockPayoutClient implements PayoutClient {

    @Override
    public PayoutResult transferPix(String pixKey, BigDecimal amount, String externalReference, String description) {
        log.info("[MOCK PAYOUT] Simulando transferência Pix: pixKey={}, amount={}, ref={}, desc={}",
                pixKey, amount, externalReference, description);

        String mockTransferId = "mock-payout-" + UUID.randomUUID();

        return new PayoutResult(
                mockTransferId,
                PayoutStatus.PAID,
                Instant.now(),
                "{\"status\":\"PAID\",\"transfer_id\":\"" + mockTransferId + "\"}",
                null
        );
    }
}
