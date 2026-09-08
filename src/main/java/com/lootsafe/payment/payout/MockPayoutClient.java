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
    public PayoutResult transferPix(String pixKey,
                                    BigDecimal amount,
                                    String externalReference,
                                    String description) {

        String id = "mock-pix-" + UUID.randomUUID();

        log.info("Simulando transferencia Pix: id={}, pixKey={}, valor={}, ref={}, desc={}",
                id, pixKey, amount, externalReference, description);

        return new PayoutResult(
                id,
                PayoutStatus.PAID,
                Instant.now(),
                "{ \"status\": \"PAID\", \"mock\": true }",
                null
        );
    }
}

