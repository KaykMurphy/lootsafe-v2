package com.lootsafe.service;

import com.lootsafe.exception.EncryptionException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RequiredArgsConstructor
@Service
public class EncryptionService {

    private static final String MSG_ENCRYPTION_FAILURE = "Falha ao criptografar os dados.";
    private static final String MSG_DECRYPTION_FAILURE = "Falha ao descriptografar os dados.";

    private final BytesEncryptor bytesEncryptor;

    public String encrypt(String plainText) {
        try {
            byte[] encrypted = bytesEncryptor.encrypt(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new EncryptionException(MSG_ENCRYPTION_FAILURE);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = bytesEncryptor.decrypt(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException(MSG_DECRYPTION_FAILURE);
        }
    }
}