package com.api.discovery.service;

import com.api.discovery.repository.ApiServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApiDiscoveryService {

    private final ApiServiceRepository repository;
    private final VectorStore vectorStore;

    public List<?> search(String query) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(10)
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    public List<Map<String, Object>> getGraphData() {
        return repository.getGraphData();
    }
}
