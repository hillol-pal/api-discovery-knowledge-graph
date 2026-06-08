package com.api.discovery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SemanticSearchRequest {

    @NotBlank(message = "Query cannot be empty")
    private String query;                    // Natural language query in plain English

    private Integer topK = 5;                // Number of results to retrieve
    private boolean includeGraphData = true; // Whether to return raw graph data
}
