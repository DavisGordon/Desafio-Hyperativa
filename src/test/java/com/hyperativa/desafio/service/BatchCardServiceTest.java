package com.hyperativa.desafio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.hyperativa.desafio.dto.BatchSummary;
import com.hyperativa.desafio.repository.CardRepository;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class BatchCardServiceTest {

    @InjectMocks
    private BatchCardService batchCardService;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private EntityManager entityManager;

    @Test
    void processFile_ShouldSaveBatch_WhenContentIsValid() {
        String validCard = "1234567812345670";
        StringBuilder fileContent = new StringBuilder();
        fileContent.append("DESAFIO-HYPERATIVA\n");
        
        // Line structure: "C" (index 0) + spaces (1-6) + Card (7-22) + padding
        String line = "C      " + validCard + "           ";
        fileContent.append(line).append("\n");
        fileContent.append("LOTE\n");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                fileContent.toString().getBytes());

        when(cardRepository.existsByNumberHash(anyString())).thenReturn(false);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        BatchSummary summary = batchCardService.processFile(file);

        assertNotNull(summary);
        assertEquals(3, summary.getTotalLinesProcessed());
        assertEquals(1, summary.getSuccessCount());
        assertEquals(0, summary.getFailedCount());

        verify(cardRepository, atLeastOnce()).saveAll(anyList());
        verify(entityManager, atLeastOnce()).clear();
    }

    @Test
    void processFile_ShouldCountFailures_WhenContentIsInvalid() {
        
    	String invalidCard = "1234567812345671"; // Invalid Luhn

        StringBuilder fileContent = new StringBuilder();
        fileContent.append("DESAFIO-HYPERATIVA\n");
        fileContent.append("C      " + invalidCard + "           \n");
        fileContent.append("LOTE\n");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                fileContent.toString().getBytes());

        BatchSummary summary = batchCardService.processFile(file);

        assertEquals(3, summary.getTotalLinesProcessed());
        assertEquals(0, summary.getSuccessCount());
        assertEquals(1, summary.getFailedCount());

        verify(cardRepository, never()).saveAll(anyList());
    }
}
