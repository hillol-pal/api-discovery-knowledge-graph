package com.api.discovery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchResponse {

    private String naturalLanguageAnswer;           // LLM-generated human readable answer
    private List<SemanticSearchResult> results;     // Structured results from graph
    private int totalResults;
    private long processingTimeMs;
}
