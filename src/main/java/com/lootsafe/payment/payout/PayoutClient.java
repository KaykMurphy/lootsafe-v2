package com.lootsafe.payment.payout;

import java.math.BigDecimal;

public interface PayoutClient {

    PayoutResult transferPix(
            String pixKey,
            BigDecimal amount,
            String externalReference,
            String description
    );

}
