package com.api.discovery.service;

import com.api.discovery.dto.SemanticSearchRequest;
import com.api.discovery.dto.SemanticSearchResponse;
import com.api.discovery.dto.SemanticSearchResult;
import com.api.discovery.repository.ApiServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiDiscoveryService {

    private final ApiServiceRepository repository;
    private final VectorStore vectorStore;

    private final ChatClient chatClient;
    private final Neo4jClient neo4jClient;

    public SemanticSearchResponse search(SemanticSearchRequest request) {
        long start = System.currentTimeMillis();

        // Step 1: Convert natural language to Cypher using LLM
        String cypherQuery = generateCypherFromNaturalLanguage(request.getQuery());

        log.info("Generated Cypher Query:\n{}", cypherQuery);

        // Step 2: Execute Cypher safely
        List<Map<String, Object>> graphResults;
        try {
            graphResults = new ArrayList<>(neo4jClient.query(cypherQuery)
                    .fetch()
                    .all());
        } catch (Exception e) {
            log.error("Cypher execution failed", e);
            return buildErrorResponse(request.getQuery(), "Query execution failed: " + e.getMessage());
        }

        if (graphResults.isEmpty()) {
            return buildNoResultsResponse(request.getQuery());
        }

        // Step 3: Convert graph results to natural language + structured response
        String naturalLanguageAnswer = generateNaturalLanguageFromGraphResults(
                request.getQuery(), graphResults);

        List<SemanticSearchResult> results = mapGraphResultsToDto(graphResults);

        return SemanticSearchResponse.builder()
                .naturalLanguageAnswer(naturalLanguageAnswer)
                .results(results)
                .totalResults(results.size())
                .processingTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    /**
     * Uses LLM to convert natural language into a safe Cypher query
     */
    private String generateCypherFromNaturalLanguage(String naturalLanguageQuery) {
        String systemPrompt = """
                You are an expert Neo4j Cypher query generator for an API Knowledge Graph.
                The graph has these node labels and relationships:

                Nodes:
                - APIService (name, title, version, description)
                - Endpoint (path, method, summary, description, operationId)
                - Parameter (name, in, type, required, description)
                - SchemaEntity (name, type, description)

                Relationships:
                - APIService -[:HAS_ENDPOINT]-> Endpoint
                - Endpoint -[:HAS_PARAMETER]-> Parameter
                - Endpoint -[:USES_SCHEMA]-> SchemaEntity

                Rules:
                - Generate only valid Cypher.
                - Always use MATCH and RETURN.
                - Limit results to maximum 10.
                - Do not use DELETE, CREATE, MERGE, or any write operations.
                - Use case-insensitive matching where appropriate.
                - Return only the Cypher query, nothing else.
                """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(naturalLanguageQuery)
                .call()
                .content()
                .trim()
                .replace("```cypher", "")
                .replace("```", "")
                .trim();
    }

    /**
     * Converts graph query results into natural language explanation
     */
    private String generateNaturalLanguageFromGraphResults(String userQuery,
                                                           List<Map<String, Object>> results) {
        String context = results.stream()
                .map(Map::toString)
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are a helpful API documentation assistant.
                Based on the following graph query results, answer the user's question in natural language.
                Be clear and concise.

                User Question: %s

                Graph Results:
                %s
                """.formatted(userQuery, context);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private List<SemanticSearchResult> mapGraphResultsToDto(List<Map<String, Object>> results) {
        return results.stream()
                .map(row -> SemanticSearchResult.builder()
                        .apiName((String) row.get("apiName"))
                        .apiVersion((String) row.get("apiVersion"))
                        .method((String) row.get("method"))
                        .path((String) row.get("path"))
                        .summary((String) row.get("summary"))
                        .description((String) row.get("description"))
                        .build())
                .collect(Collectors.toList());
    }

    private SemanticSearchResponse buildNoResultsResponse(String query) {
        return SemanticSearchResponse.builder()
                .naturalLanguageAnswer("No matching APIs found in the knowledge graph for: \"" + query + "\"")
                .results(List.of())
                .totalResults(0)
                .build();
    }

    private SemanticSearchResponse buildErrorResponse(String query, String error) {
        return SemanticSearchResponse.builder()
                .naturalLanguageAnswer("Sorry, I encountered an error while querying the knowledge graph.")
                .results(List.of())
                .totalResults(0)
                .build();
    }

    public List<Map<String, Object>> getGraphData() {
        return repository.getGraphData();
    }
}
