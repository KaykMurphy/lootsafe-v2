package com.lootsafe.config;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.order.OrderClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MercadoPagoSdkConfig {

    private final MercadoPagoProperties properties;

    @Bean
    public OrderClient orderClient() {
        return new OrderClient();
    }

    @PostConstruct
    void configureMercadoPagoSdk() {
        MercadoPagoConfig.setAccessToken(properties.getAccessToken());
    }
}
