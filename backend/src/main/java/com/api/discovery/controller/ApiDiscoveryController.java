package com.api.discovery.controller;


import com.api.discovery.dto.IngestionResult;
import com.api.discovery.dto.SemanticSearchRequest;
import com.api.discovery.dto.SemanticSearchResponse;
import com.api.discovery.service.ApiDiscoveryService;
import com.api.discovery.service.ApiIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiDiscoveryController {

    private final ApiIngestionService ingestionService;
    private final ApiDiscoveryService discoveryService;

    public ApiDiscoveryController(ApiIngestionService ingestionService, ApiDiscoveryService discoveryService) {
        this.ingestionService = ingestionService;
        this.discoveryService = discoveryService;
    }


    @PostMapping("/ingest")
    public ResponseEntity<String> ingestApi(
            @RequestParam String apiName,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) MultipartFile file) {

        IngestionResult result = ingestionService.ingest(apiName, url, file);
        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(result.getMessage());
        }
        return ResponseEntity.ok(result.getMessage() + " : " + result.getApiName() + " : " + result.getApiId());
    }

    @PostMapping("/discover/semantic")
    public ResponseEntity<SemanticSearchResponse> semanticSearch(
            @Valid @RequestBody SemanticSearchRequest request) {
        return ResponseEntity.ok(discoveryService.search(request));
    }

    @GetMapping("/graph/data")
    public ResponseEntity<List<Map<String, Object>>> getGraphData() {
        return ResponseEntity.ok(discoveryService.getGraphData());
    }
}
