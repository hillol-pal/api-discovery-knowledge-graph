package com.api.discovery.controller;


import com.api.discovery.service.ApiDiscoveryService;
import com.api.discovery.service.ApiIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiDiscoveryController {

    private final ApiIngestionService ingestionService;
    private final ApiDiscoveryService discoveryService;

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestApi(
            @RequestParam String apiName,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) MultipartFile file) {

        String result = ingestionService.ingest(apiName, url, file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/discover/semantic")
    public ResponseEntity<List<?>> semanticSearch(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(discoveryService.search(body.get("query")));
    }

    @GetMapping("/graph/data")
    public ResponseEntity<List<Map<String, Object>>> getGraphData() {
        return ResponseEntity.ok(discoveryService.getGraphData());
    }
}
