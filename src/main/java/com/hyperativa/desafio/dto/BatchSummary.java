package com.hyperativa.desafio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatchSummary {
    private int totalLinesProcessed;
    private int successCount;
    private int failedCount;
    private long durationMs;
}
