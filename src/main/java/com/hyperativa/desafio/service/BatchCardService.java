package com.hyperativa.desafio.service;

import com.hyperativa.desafio.domain.Card;
import com.hyperativa.desafio.dto.BatchSummary;
import com.hyperativa.desafio.repository.CardRepository;
import com.hyperativa.desafio.util.CardUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchCardService {

    private final CardRepository cardRepository;
    private final EncryptionService encryptionService;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private final EntityManager entityManager;

    private static final int BATCH_SIZE = 500;

    public BatchSummary processFile(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        int totalLinesProcessed = 0;
        int successCount = 0;
        int failedCount = 0;

        List<Card> buffer = new ArrayList<>(BATCH_SIZE);
        Set<String> batchHashes = new HashSet<>(BATCH_SIZE);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                totalLinesProcessed++;
                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty())
                    continue;

                // 1. Skip Header
                if (trimmedLine.startsWith("DESAFIO-HYPERATIVA")) {
                    continue;
                }

                // 2. Stop at Trailer
                if (trimmedLine.startsWith("LOTE")) {
                    continue;
                }

                // 3. Process Data Lines
                if (trimmedLine.startsWith("C")) {
                    try {
                        processLine(trimmedLine, buffer, batchHashes);
                    } catch (Exception e) {
                        failedCount++;
                        log.warn("Failed to process line: {}. Error: {}", trimmedLine, e.getMessage());
                    }
                }

                // Flush buffer if full
                if (buffer.size() >= BATCH_SIZE) {
                    BatchResult result = saveBatch(buffer);
                    successCount += result.savedCount;
                    failedCount += result.failedCount;
                    // Clear batch tracking
                    batchHashes.clear();
                }
            }

            // Flush remaining
            if (!buffer.isEmpty()) {
                BatchResult result = saveBatch(buffer);
                successCount += result.savedCount;
                failedCount += result.failedCount;
                batchHashes.clear();
            }

        } catch (IOException e) {
            throw new RuntimeException("Error reading file", e);
        }

        long duration = System.currentTimeMillis() - startTime;

        return BatchSummary.builder()
                .totalLinesProcessed(totalLinesProcessed)
                .successCount(successCount)
                .failedCount(failedCount)
                .durationMs(duration)
                .build();
    }

    private void processLine(String line, List<Card> buffer, Set<String> batchHashes) {
        // Line structure: "C" + ...
        // Index 8-26 (1-based) => 7-26 (0-based)

        String rawCard;
        try {
            int start = 7;
            int end = 26;

            if (line.length() < start) {
                throw new IllegalArgumentException("Line too short");
            }

            rawCard = line.substring(start, Math.min(line.length(), end)).trim();

            if (rawCard.isEmpty()) {
                throw new IllegalArgumentException("Empty card number");
            }

        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid line format", e);
        }

        // Validate Luhn
        if (!CardUtils.isLuhnValid(rawCard)) {
            throw new IllegalArgumentException("Invalid Luhn");
        }

        // Generate Hash
        String numberHash = CardUtils.generateHash(rawCard);

        // Check duplicate within global DB
        if (cardRepository.existsByNumberHash(numberHash)) {
            throw new IllegalArgumentException("Duplicate card (DB)");
        }

        // Check duplicate within current batch logic
        // We use a Set for O(1) lookup in the current batch
        if (batchHashes.contains(numberHash)) {
            throw new IllegalArgumentException("Duplicate in current batch");
        }

        // Encrypt
        String encrypted = encryptionService.encrypt(rawCard);

        // Add to buffer
        Card card = Card.builder()
                .encryptedNumber(encrypted)
                .numberHash(numberHash)
                .build();

        buffer.add(card);
        batchHashes.add(numberHash);
    }

    private record BatchResult(int savedCount, int failedCount) {
    }

    private BatchResult saveBatch(List<Card> buffer) {
        if (buffer.isEmpty()) {
            return new BatchResult(0, 0);
        }

        int size = buffer.size();
        try {
            transactionTemplate.execute(status -> {
                cardRepository.saveAll(buffer);
                entityManager.flush();
                entityManager.clear(); // Detach entities to free memory
                return null;
            });
            buffer.clear();
            return new BatchResult(size, 0);
        } catch (Exception e) {
            log.error("Failed to save batch of size {}. Error: {}", size, e.getMessage());
            buffer.clear();
            // If the batch fails, all items in it constitute a failure
            return new BatchResult(0, size);
        }
    }
}
