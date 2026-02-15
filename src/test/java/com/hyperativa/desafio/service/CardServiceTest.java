package com.hyperativa.desafio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hyperativa.desafio.domain.Card;
import com.hyperativa.desafio.dto.CardRequest;
import com.hyperativa.desafio.dto.CardResponse;
import com.hyperativa.desafio.exception.DuplicateCardException;
import com.hyperativa.desafio.repository.CardRepository;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @InjectMocks
    private CardService cardService;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private EncryptionService encryptionService;

    private static final String VALID_CARD_NUMBER = "1234567812345670"; // Luhn Valid
    private static final String INVALID_CARD_NUMBER = "1234567812345671"; // Luhn Invalid

    @Test
    void create_ShouldSucceed_WhenCardIsValid() {
        CardRequest request = new CardRequest();
        request.setCardNumber(VALID_CARD_NUMBER);

        when(cardRepository.existsByNumberHash(anyString())).thenReturn(false);
        when(encryptionService.encrypt(VALID_CARD_NUMBER)).thenReturn("encrypted-value");

        Card savedCard = Card.builder()
                .id(UUID.randomUUID())
                .encryptedNumber("encrypted-value")
                .numberHash("hash-value")
                .createdAt(LocalDateTime.now())
                .build();

        when(cardRepository.save(any(Card.class))).thenReturn(savedCard);

        CardResponse response = cardService.create(request);

        assertNotNull(response);
        assertEquals(savedCard.getId(), response.getId());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void create_ShouldThrowException_WhenLuhnIsInvalid() {
        CardRequest request = new CardRequest();
        request.setCardNumber(INVALID_CARD_NUMBER);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            cardService.create(request);
        });

        assertEquals("Invalid card number (Luhn check failed)", exception.getMessage());
        verify(cardRepository, never()).save(any());
    }

    @Test
    void create_ShouldThrowException_WhenDuplicateCard() {
        CardRequest request = new CardRequest();
        request.setCardNumber(VALID_CARD_NUMBER);

        when(cardRepository.existsByNumberHash(anyString())).thenReturn(true);

        assertThrows(DuplicateCardException.class, () -> {
            cardService.create(request);
        });

        verify(cardRepository, never()).save(any());
    }

    @Test
    void findByCardNumber_ShouldReturnResponse_WhenCardExists() {
        Card card = Card.builder()
                .id(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();

        when(cardRepository.findByNumberHash(anyString())).thenReturn(Optional.of(card));

        CardResponse response = cardService.findByCardNumber(VALID_CARD_NUMBER);

        assertNotNull(response);
        assertEquals(card.getId(), response.getId());
    }

    @Test
    void findByCardNumber_ShouldThrowException_WhenCardNotFound() {
        when(cardRepository.findByNumberHash(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            cardService.findByCardNumber(VALID_CARD_NUMBER);
        });
    }
}
