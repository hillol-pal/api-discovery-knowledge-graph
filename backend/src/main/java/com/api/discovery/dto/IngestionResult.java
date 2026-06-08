package com.api.discovery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResult {

    private boolean success;
    private String message;
    private Long apiId;
    private String apiName;
    private Instant timestamp;

    // Factory methods - clean and fluent

    public static IngestionResult success(String apiName, Long apiId) {
        return new IngestionResult(true,
                "API ingested successfully: " + apiName,
                apiId,
                apiName,
                Instant.now());
    }

    public static IngestionResult failure(String message) {
        return new IngestionResult(false,
                "Ingestion failed: " + message,
                null, null, Instant.now());
    }

    public static IngestionResult failure(String message, Exception e) {
        return new IngestionResult(false,
                "Ingestion failed: " + message + " - " + e.getMessage(),
                null, null, Instant.now());
    }
}
