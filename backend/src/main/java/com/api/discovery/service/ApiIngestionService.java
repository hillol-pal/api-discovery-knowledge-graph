package com.api.discovery.service;

import com.api.discovery.dto.IngestionResult;
import com.api.discovery.model.APIService;
import com.api.discovery.model.Endpoint;
import com.api.discovery.model.Parameter;
import com.api.discovery.repository.ApiServiceRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@Slf4j
public class ApiIngestionService {

    private final ApiServiceRepository repository;
    private final EmbeddingService embeddingService;
    private final RestClient restClient;
    private final OpenAPIV3Parser openAPIV3Parser;

    public ApiIngestionService(ApiServiceRepository repository, EmbeddingService embeddingService,
                               RestClient restClient, OpenAPIV3Parser openAPIV3Parser) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.restClient = restClient;
        this.openAPIV3Parser = openAPIV3Parser;
    }


    public IngestionResult ingest(String apiName, String url, MultipartFile file) {
        try {
            String content = readContent(file,url);

            SwaggerParseResult parseResult= openAPIV3Parser.readContents(content, null, null);

            if (parseResult.getOpenAPI() == null) {
                return IngestionResult.failure("Invalid OpenAPI document: " + parseResult.getMessages());
            }

            OpenAPI openAPI = parseResult.getOpenAPI();
            // Idempotency
            Optional<APIService> existing = repository.findByNameAndVersion(apiName, openAPI.getInfo().getVersion());
            APIService apiService = existing.orElseGet(APIService::new);

            mapBasicInfo(apiService, apiName, openAPI);
            mapEndpoints(apiService, openAPI);

            embeddingService.generateEmbeddingForApi(apiService);
            apiService.getEndpoints().forEach(embeddingService::generateEmbeddingForEndpoint);

            repository.save(apiService);

            return IngestionResult.success(apiName, apiService.getId());

        } catch (Exception e) {
            log.error("API ingestion failed for: {}", apiName, e);
            return IngestionResult.failure(e.getMessage());
        }
    }

    private String readContent(MultipartFile file, String url) throws IOException {
        if (file != null && !file.isEmpty()) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        if (StringUtils.hasText(url)) {
            return restClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
        }
        throw new IllegalArgumentException("Either file or url must be provided");
    }

    private void mapBasicInfo(APIService api, String apiName, OpenAPI openAPI) {
        api.setName(apiName);
        api.setTitle(openAPI.getInfo().getTitle());
        api.setVersion(openAPI.getInfo().getVersion());
        api.setDescription(openAPI.getInfo().getDescription());
    }

    private void mapEndpoints(APIService apiService, OpenAPI openAPI) {
        apiService.getEndpoints().clear(); // For update case

        if (openAPI.getPaths() == null) return;

        openAPI.getPaths().forEach((pathStr, pathItem) -> {
            mapPathItem(apiService, pathStr, pathItem);
        });
    }

    private void mapPathItem(APIService apiService, String pathStr, PathItem pathItem) {
        mapOperation(apiService, pathStr, "GET", pathItem.getGet());
        mapOperation(apiService, pathStr, "POST", pathItem.getPost());
        mapOperation(apiService, pathStr, "PUT", pathItem.getPut());
        mapOperation(apiService, pathStr, "DELETE", pathItem.getDelete());
    }

    private void mapOperation(APIService apiService, String path, String method, Operation operation) {
        if (operation == null) return;

        Endpoint endpoint = new Endpoint();
        endpoint.setPath(path);
        endpoint.setMethod(method);
        endpoint.setSummary(operation.getSummary());
        endpoint.setDescription(operation.getDescription());
        endpoint.setOperationId(operation.getOperationId());

        // Parameters
        if (operation.getParameters() != null) {
            operation.getParameters().forEach(param -> {
                Parameter p = mapParameter(param);
                p.setEndpoint(endpoint);
                endpoint.getParameters().add(p);
            });
        }

        // Request Body Schema
        if (operation.getRequestBody() != null) {
            // Extract and map schema (complex logic - can be expanded)
        }

        // Responses
        if (operation.getResponses() != null) {
            // Map 200, 201, etc. response schemas
        }

        apiService.getEndpoints().add(endpoint);
    }

    private Parameter mapParameter(io.swagger.v3.oas.models.parameters.Parameter param) {
        Parameter p = new Parameter();
        p.setName(param.getName());
        p.setIn(param.getIn());
        p.setDescription(param.getDescription());
        p.setRequired(param.getRequired());
        // Map schema type/format/example...
        if (param.getSchema() != null) {
            p.setType(param.getSchema().getType());
            p.setFormat(param.getSchema().getFormat());
        }
        return p;
    }
}
