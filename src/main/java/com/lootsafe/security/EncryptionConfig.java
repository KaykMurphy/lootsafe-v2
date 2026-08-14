package com.lootsafe.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;


@Configuration
@RequiredArgsConstructor
public class EncryptionConfig {

    private final EncryptionProperties properties;

    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.text(properties.getPassword(), properties.getSalt());
    }
}
