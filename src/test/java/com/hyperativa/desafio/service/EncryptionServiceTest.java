package com.hyperativa.desafio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EncryptionServiceTest {

    private EncryptionService encryptionService;
    private final String TEST_SECRET = "5vMCw0th3/8uX1+Qj5/Zk9l8vMCw0th3/8uX1+Qj5/Y="; // 32 bytes (Base64)

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService(TEST_SECRET);
    }

    @Test
    void encryptAndDecrypt_ShouldRestoreOriginalString() {
        String originalText = "1234567890123456";

        String encrypted = encryptionService.encrypt(originalText);
        String decrypted = encryptionService.decrypt(encrypted);

        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void encrypt_ShouldReturnDifferentString_ForSameInput_DueToRandomIV() {
        String originalText = "SuperSecretData";

        String encrypted1 = encryptionService.encrypt(originalText);
        String encrypted2 = encryptionService.encrypt(originalText);

        assertNotEquals(encrypted1, encrypted2,
                "Encryption should use random IV, producing different outputs for same input");
    }

    @Test
    void decrypt_ShouldThrowException_WhenKeyIsWrong() {
        String originalText = "CriticalData";
        String encrypted = encryptionService.encrypt(originalText);

        EncryptionService wrongKeyService = new EncryptionService("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");

        assertThrows(SecurityException.class, () -> wrongKeyService.decrypt(encrypted));
    }

    @Test
    void decrypt_ShouldThrowException_WhenDataIsTampered() {
        String originalText = "IntegrityCheck";
        String encrypted = encryptionService.encrypt(originalText);

        // Tamper with the encrypted string (base64)
        // We'll flip a bit in the middle
        byte[] bytes = Base64.getDecoder().decode(encrypted);
        bytes[bytes.length - 5] ^= 1; // Flip a bit in the ciphertext part
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThrows(SecurityException.class, () -> encryptionService.decrypt(tampered));
    }
}
