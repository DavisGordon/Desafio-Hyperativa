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
        Set<String> batchHashes = HashSet.newHashSet(BATCH_SIZE);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                totalLinesProcessed++;
                String trimmedLine = line.trim();

                if (shouldSkip(trimmedLine)) continue;

                if (trimmedLine.startsWith("C")) {
                    try {
                        Card card = parseAndValidate(trimmedLine, batchHashes);

                        buffer.add(card);
                        batchHashes.add(card.getNumberHash());

                    } catch (Exception e) {
                        failedCount++;
                        log.debug("Validation error line {}: {}", totalLinesProcessed, e.getMessage());
                    }
                }

                if (buffer.size() >= BATCH_SIZE) {
                    BatchResult result = processBatch(buffer);
                    successCount += result.savedCount;
                    failedCount += result.failedCount;
                    
                    // Prepare for the next batch
                    buffer.clear();
                    batchHashes.clear();
                }
            }

            // Final flush (remaining items)
            if (!buffer.isEmpty()) {
                BatchResult result = processBatch(buffer);
                successCount += result.savedCount;
                failedCount += result.failedCount;
            }

        } catch (IOException e) {
            log.error("IO Error processing file", e);
            throw new RuntimeException("Error processing file", e);
        }

        return BatchSummary.builder()
                .totalLinesProcessed(totalLinesProcessed)
                .successCount(successCount)
                .failedCount(failedCount)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    // Helper method to avoid code duplication (DRY) and clear memory
    private BatchResult processBatch(List<Card> buffer) {
        BatchResult result = flushBuffer(buffer);
        entityManager.clear(); // CRITICAL: Detach entities to free up Hibernate memory after each batch
        return result;
    }

    private boolean shouldSkip(String line) {
        return line.isEmpty() || line.startsWith("DESAFIO-HYPERATIVA") || line.startsWith("LOTE");
    }

    private Card parseAndValidate(String line, Set<String> batchHashes) {
        // Validate Length
        int start = 7;
        int end = 26;
        if (line.length() < start) throw new IllegalArgumentException("Line too short");

        String rawCard = line.substring(start, Math.min(line.length(), end)).trim();
        if (rawCard.isEmpty()) throw new IllegalArgumentException("Empty card number");

        // Validate Luhn
        if (!CardUtils.isLuhnValid(rawCard)) throw new IllegalArgumentException("Invalid Luhn");

        // Generate Hash
        String numberHash = CardUtils.generateHash(rawCard);

        // Validate Duplicates in current BATCH
        if (batchHashes.contains(numberHash)) throw new IllegalArgumentException("Duplicate in current batch");

        return Card.builder()
                .encryptedNumber(encryptionService.encrypt(rawCard))
                .numberHash(numberHash)
                .build();
    }

    private record BatchResult(int savedCount, int failedCount) {}

    private BatchResult flushBuffer(List<Card> buffer) {
        if (buffer.isEmpty()) return new BatchResult(0, 0);

        try {
            // Happy Path: Try to save the whole batch at once
            return transactionTemplate.execute(status -> {
                cardRepository.saveAll(buffer);
                entityManager.flush();
                return new BatchResult(buffer.size(), 0);
            });
        } catch (Exception e) {
            // Resilience Path: Something went wrong, switch to item-by-item saving
            log.warn("Batch failed (possibly duplicates). Switching to item-by-item processing.");
            return saveIndividually(buffer);
        }
    }

    private BatchResult saveIndividually(List<Card> buffer) {
        int saved = 0;
        int failed = 0;

        for (Card card : buffer) {
            try {
                // New isolated transaction for each item
                transactionTemplate.execute(status -> {
                    cardRepository.save(card);
                    return null;
                });
                saved++;
            } catch (Exception ex) {
                failed++;
                log.debug("Failed to save card {}: {}", card.getNumberHash(), ex.getMessage());
            }
        }
        return new BatchResult(saved, failed);
    }
}