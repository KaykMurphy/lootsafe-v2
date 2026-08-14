package com.lootsafe.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "encryption")
public class EncryptionProperties {

    @NotBlank(message = "A senha de criptografia (ENCRYPTION_PASSWORD) é obrigatória no ambiente.")
    private String password;

    @NotBlank(message = "O salt de criptografia (ENCRYPTION_SALT) é obrigatório no ambiente.")
    private String salt;
}