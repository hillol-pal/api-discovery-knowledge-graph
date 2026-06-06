package com.api.discovery.service;



import com.api.discovery.model.APIService;
import com.api.discovery.repository.ApiServiceRepository;
import io.swagger.v3.parser.OpenAPIV3Parser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiIngestionService {

    private final ApiServiceRepository repository;
    private final EmbeddingModel embeddingModel;

    public String ingest(String apiName, String url, MultipartFile file) {
        try {
            String content = file != null ? new String(file.getBytes()) :
                    new org.springframework.web.client.RestTemplate().getForObject(url, String.class);

            var result = new OpenAPIV3Parser().readContents(content, null, null);
            var openAPI = result.getOpenAPI();

            if (openAPI == null) {
                return "Invalid OpenAPI document";
            }

            APIService api = new APIService();
            api.setName(apiName);
            api.setTitle(openAPI.getInfo().getTitle());
            api.setVersion(openAPI.getInfo().getVersion());
            api.setDescription(openAPI.getInfo().getDescription());

            // Map endpoints + generate embeddings...
            // (You can expand this logic as needed)

            repository.save(api);
            return "API ingested successfully: " + apiName;

        } catch (Exception e) {
            log.error("Ingestion failed", e);
            return "Failed to ingest API: " + e.getMessage();
        }
    }
}
