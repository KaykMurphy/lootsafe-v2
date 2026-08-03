package com.lootsafe.service;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import com.lootsafe.exception.EncryptionException;

@AllArgsConstructor
@Service
public class EncryptionService {

    private final TextEncryptor textEncryptor;

    public String encrypt(String plainText) {
        try {
            return textEncryptor.encrypt(plainText);
        } catch (Exception e) {
            throw new EncryptionException("Falha ao criptografar os dados.");
        }
    }

    public String decrypt(String encryptedText) {
        try {
            return textEncryptor.decrypt(encryptedText);
        } catch (Exception e) {
            throw new EncryptionException("Falha ao descriptografar os dados.");
        }
    }


}
