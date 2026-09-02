package com.lootsafe.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@ConfigurationProperties(prefix = "inspection")
public class InspectionProperties {


    @NotNull(message = "O tempo padrão de inspeção não pode ser nulo")
    @Positive(message = "O tempo padrão de inspeção deve ser maior que zero")
    private BigDecimal defaultHours;


    @NotNull(message = "O tempo mínimo de inspeção não pode ser nulo")
    @Positive(message = "O tempo mínimo de inspeção deve ser maior que zero")
    private BigDecimal minHours;


    @NotNull(message = "O tempo máximo de inspeção não pode ser nulo")
    @Positive(message = "O tempo máximo de inspeção deve ser maior que zero")
    private BigDecimal maxHours;


    @NotNull(message = "O intervalo de auto-release não pode ser nulo")
    @Positive(message = "O intervalo de auto-release deve ser maior que zero milissegundos")
    private Long autoReleaseIntervalMs;

}