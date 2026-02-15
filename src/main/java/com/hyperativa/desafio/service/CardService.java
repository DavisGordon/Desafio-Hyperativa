package com.hyperativa.desafio.service;

import com.hyperativa.desafio.domain.Card;
import com.hyperativa.desafio.dto.CardRequest;
import com.hyperativa.desafio.dto.CardResponse;
import com.hyperativa.desafio.exception.DuplicateCardException;
import com.hyperativa.desafio.repository.CardRepository;
import com.hyperativa.desafio.util.CardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final EncryptionService encryptionService;

    public CardResponse create(CardRequest request) {
        String cardNumber = request.getCardNumber();

        // Validate Card Number
        if (!CardUtils.isLuhnValid(cardNumber)) {
            throw new IllegalArgumentException("Invalid card number (Luhn check failed)");
        }

        // Generate SHA-256 Hash
        String numberHash = CardUtils.generateHash(cardNumber);

        // Check for duplicates
        if (cardRepository.existsByNumberHash(numberHash)) {
            throw new DuplicateCardException("Card already registered");
        }

        // Encrypt the card number
        String encryptedNumber = encryptionService.encrypt(cardNumber);

        // Save Card entity
        Card card = Card.builder()
                .encryptedNumber(encryptedNumber)
                .numberHash(numberHash)
                .build();

        Card savedCard = cardRepository.save(card);

        // Return CardResponse
        return CardResponse.builder()
                .id(savedCard.getId())
                .createdAt(savedCard.getCreatedAt())
                .build();
    }

    public CardResponse getCard(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        return CardResponse.builder()
                .id(card.getId())
                .createdAt(card.getCreatedAt())
                .build();
    }

    public CardResponse findByCardNumber(String plainCardNumber) {
        String numberHash = CardUtils.generateHash(plainCardNumber);
        Card card = cardRepository.findByNumberHash(numberHash)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        return CardResponse.builder()
                .id(card.getId())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
