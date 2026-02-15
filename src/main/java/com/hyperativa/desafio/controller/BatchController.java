package com.hyperativa.desafio.controller;

import com.hyperativa.desafio.dto.BatchSummary;
import com.hyperativa.desafio.service.BatchCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class BatchController {

    private final BatchCardService batchCardService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchSummary> uploadFile(@RequestParam("file") MultipartFile file) {
        BatchSummary summary = batchCardService.processFile(file);
        return ResponseEntity.ok(summary);
    }
}
