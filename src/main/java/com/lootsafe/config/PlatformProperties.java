package com.lootsafe.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "platform")
public class PlatformProperties {


    @NotNull(message = "O percentual da taxa não pode ser nulo")
    @PositiveOrZero(message = "O percentual da taxa deve ser zero ou positivo")
    private BigDecimal percentage;

    @NotNull(message = "A taxa fixa não pode ser nula")
    @PositiveOrZero(message = "A taxa fixa deve ser zero ou positiva")
    private BigDecimal feeFixed;


}
