package com.lootsafe.service;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import com.lootsafe.exception.EncryptionException;

@AllArgsConstructor
@Service
public class EncryptionService {

    private static final String MSG_ENCRYPTION_FAILURE = "Falha ao criptografar os dados.";
    private static final String MSG_DECRYPTION_FAILURE = "Falha ao descriptografar os dados.";

    private final TextEncryptor textEncryptor;

    public String encrypt(String plainText) {
        try {
            return textEncryptor.encrypt(plainText);
        } catch (Exception e) {
            throw new EncryptionException(MSG_ENCRYPTION_FAILURE);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            return textEncryptor.decrypt(encryptedText);
        } catch (Exception e) {
            throw new EncryptionException(MSG_DECRYPTION_FAILURE);
        }
    }


}
