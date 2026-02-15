package com.hyperativa.desafio.controller;

import com.hyperativa.desafio.dto.CardRequest;
import com.hyperativa.desafio.dto.CardResponse;
import com.hyperativa.desafio.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardResponse> create(@RequestBody @Valid CardRequest request) {
        CardResponse response = cardService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCard(@PathVariable UUID id) {
        CardResponse response = cardService.getCard(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<CardResponse> search(@RequestParam("number") String number) {
        CardResponse response = cardService.findByCardNumber(number);
        return ResponseEntity.ok(response);
    }
}
