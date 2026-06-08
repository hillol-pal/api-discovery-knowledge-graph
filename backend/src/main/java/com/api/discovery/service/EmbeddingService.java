package com.api.discovery.service;

import com.api.discovery.model.APIService;
import com.api.discovery.model.Endpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private static final int EMBEDDING_DIMENSION = 1536;

    /**
     * Generates and stores embedding for the entire APIService (title + description)
     */
    @Transactional
    public void generateEmbeddingForApi(APIService apiService) {
        String embeddingText = buildApiEmbeddingText(apiService);

        if (embeddingText.isBlank()) {
            log.warn("No content to embed for API: {}", apiService.getName());
            return;
        }

        apiService.setEmbeddingText(embeddingText);
       // float[] embedding = generateEmbedding(embeddingText);
       // apiService.setEmbedding(embedding);

    }

    /**
     * Generates and stores embedding for an Endpoint (most important for search)
     */
    @Transactional
    public void generateEmbeddingForEndpoint(Endpoint endpoint) {
        String embeddingText = buildEndpointEmbeddingText(endpoint);

        if (embeddingText.isBlank()) {
            return;
        }

        endpoint.setEmbeddingText(embeddingText);
        //float[] embedding = generateEmbedding(embeddingText);
        //endpoint.setEmbedding(embedding);

        log.debug("Generated embedding for endpoint: {} {}", endpoint.getMethod(), endpoint.getPath());
    }

    /**
     * Generate embedding using Spring AI EmbeddingModel
     */
   /* public float[] generateEmbedding(String text) {
        try {
            EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
            EmbeddingResponse response = embeddingModel.embedForResponse(request);

            if (response.getResults().isEmpty()) {
                throw new RuntimeException("Empty embedding response");
            }

            return response.getResult().getOutput();
        } catch (Exception e) {
            log.error("Failed to generate embedding for text: {}", text.substring(0, Math.min(100, text.length())), e);
            // Return zero vector as fallback (or throw depending on policy)
            return new float[EMBEDDING_DIMENSION];
        }
    }*/

    /**
     * Build rich embedding text for APIService
     */
    private String buildApiEmbeddingText(APIService api) {
        return String.join(" | ",
                "API: " + api.getTitle(),
                "Name: " + api.getName(),
                "Version: " + api.getVersion(),
                "Description: " + (api.getDescription() != null ? api.getDescription() : "")
        ).trim();
    }

    /**
     * Build rich embedding text for Endpoint (this is what users will search against)
     */
    private String buildEndpointEmbeddingText(Endpoint endpoint) {
        StringBuilder sb = new StringBuilder();

        sb.append("Method: ").append(endpoint.getMethod()).append(" ");
        sb.append("Path: ").append(endpoint.getPath()).append(" ");

        if (endpoint.getSummary() != null) {
            sb.append("Summary: ").append(endpoint.getSummary()).append(" ");
        }
        if (endpoint.getDescription() != null) {
            sb.append("Description: ").append(endpoint.getDescription()).append(" ");
        }
        if (endpoint.getOperationId() != null) {
            sb.append("OperationId: ").append(endpoint.getOperationId());
        }

        // Add parameter info
        if (!endpoint.getParameters().isEmpty()) {
            sb.append(" Parameters: ");
            String params = endpoint.getParameters().stream()
                    .map(p -> p.getName() + " (" + p.getIn() + ")")
                    .collect(Collectors.joining(", "));
            sb.append(params);
        }

        return sb.toString().trim();
    }

    /**
     * Optional: Bulk generate embeddings for all endpoints of an API
     */
    public void generateEmbeddingsForApi(APIService apiService) {
        generateEmbeddingForApi(apiService);
        apiService.getEndpoints().forEach(this::generateEmbeddingForEndpoint);
    }

    /**
     * Search similar endpoints using vector similarity
     */
   /* public List<Endpoint> searchSimilarEndpoints(String query, int limit) {
        float[] queryEmbedding = generateEmbedding(query);

        // You can use custom Cypher query via repository or VectorStore
        // For now, returning empty - implement based on your needs
        return List.of(); // Replace with actual vector search
    }*/
}
