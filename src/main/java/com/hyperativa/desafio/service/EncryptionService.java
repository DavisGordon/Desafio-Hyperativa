package com.hyperativa.desafio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptionService(@Value("${security.jwt.secret}") String secretKeyStr) {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyStr);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Key must be 256 bits (32 bytes)");
        }
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String data) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Concatenate IV + Ciphertext
            byte[] finalMessage = new byte[GCM_IV_LENGTH + encryptedBytes.length];
            System.arraycopy(iv, 0, finalMessage, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedBytes, 0, finalMessage, GCM_IV_LENGTH, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(finalMessage);
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting data", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            byte[] decodedMessage = Base64.getDecoder().decode(encryptedData);

            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decodedMessage, 0, iv, 0, GCM_IV_LENGTH);

            // Extract Ciphertext
            int encryptedSize = decodedMessage.length - GCM_IV_LENGTH;
            byte[] encryptedBytes = new byte[encryptedSize];
            System.arraycopy(decodedMessage, GCM_IV_LENGTH, encryptedBytes, 0, encryptedSize);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SecurityException("Error while decrypting data", e);
        }
    }
}
