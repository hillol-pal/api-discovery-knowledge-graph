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
public class SemanticSearchResult {

    private String apiName;
    private String apiVersion;
    private String method;
    private String path;
    private String summary;
    private String description;
    private double similarityScore;
    private List<String> parameters;           // Simplified for response
}
