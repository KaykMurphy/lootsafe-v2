package com.lootsafe.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;


@Configuration
public class EncryptionConfig {

    @Value("${encryption.password:dev-password}")
    private String encryptionPassword;

    @Value("${encryption.salt:deadbeefdeadbeef}")
    private String encryptionSalt;

    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.text(encryptionPassword, encryptionSalt);
    }
}
