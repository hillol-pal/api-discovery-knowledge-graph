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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@Slf4j
public class ApiIngestionService {

    private final ApiServiceRepository repository;
    private final EmbeddingService embeddingService;
    private final OpenAPIV3Parser openAPIV3Parser;

    public ApiIngestionService(ApiServiceRepository repository, EmbeddingService embeddingService,
                               OpenAPIV3Parser openAPIV3Parser) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.openAPIV3Parser = openAPIV3Parser;
    }


    public IngestionResult ingest(String apiName, String url, MultipartFile file) {
        try {
            SwaggerParseResult parseResult;

            if (file != null && !file.isEmpty()) {
                // File upload path (most reliable)
                String content = new String(file.getBytes(), StandardCharsets.UTF_8);
                if (content.trim().startsWith("<")) {
                    log.warn("File upload for {} appears to contain HTML (first 120 chars): {}",
                            apiName, content.substring(0, Math.min(120, content.length())));
                    return IngestionResult.failure(
                            "Uploaded file appears to be an HTML page or error document instead of a JSON/YAML OpenAPI spec. " +
                            "Please upload a valid OpenAPI document (starts with 'openapi:' or '{ \"openapi\": ...').");
                }
                parseResult = openAPIV3Parser.readContents(content, null, null);
            } else if (StringUtils.hasText(url)) {
                // Robust fetch (follows redirects, sets proper headers) then feed to parser.
                // This prevents 302 HTML bodies, error pages, etc. from ever reaching Jackson/Swagger parser.
                String content = fetchUrlContent(url);
                if (content.trim().startsWith("<")) {
                    // Should have been caught inside fetchUrlContent, but double-check
                    log.warn("URL fetch for {} returned HTML-like content (first 120): {}",
                            apiName, content.substring(0, Math.min(120, content.length())));
                    return IngestionResult.failure(
                            "The URL returned an HTML page (likely a redirect, auth wall, or error page) instead of an OpenAPI document. " +
                            "Try uploading a local copy of the spec file instead of a public URL.");
                }
                parseResult = openAPIV3Parser.readContents(content, null, null);
            } else {
                return IngestionResult.failure("Either a file or a URL must be provided");
            }

            if (parseResult.getOpenAPI() == null) {
                String messages = String.join(", ", parseResult.getMessages());
                log.error("OpenAPI parse failed for '{}'. Parser messages: {}", apiName, messages);
                return IngestionResult.failure(
                        "The document is not a valid OpenAPI/Swagger specification (missing 'openapi' or 'swagger' root attribute). " +
                        "Verify the file/URL contents and try uploading a local copy of the spec. Details: " + messages);
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
            // Sanitize for the user; the full stack is already in the log
            return IngestionResult.failure("Failed to ingest OpenAPI document: " + e.getMessage());
        }
    }

    /**
     * Fetch remote OpenAPI content using java.net.http.HttpClient.
     * Explicitly follows redirects (NORMAL) and sends sensible Accept + User-Agent headers.
     * Throws on non-success status or obviously bad (e.g. HTML) bodies so callers can give friendly messages.
     */
    private String fetchUrlContent(String url) throws IOException, InterruptedException {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                .header("Accept", "application/json, application/yaml, text/yaml, */*")
                .header("User-Agent", "api-discovery-backend/1.0")
                .timeout(java.time.Duration.ofSeconds(15))
                .GET()
                .build();

        java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 400) {
            throw new IOException("HTTP " + resp.statusCode() + " from remote URL " + url);
        }

        String body = resp.body();
        if (body == null || body.trim().isEmpty()) {
            throw new IOException("Empty response body from " + url);
        }

        // Early detection of the classic 302/error-page HTML body problem (prevents Jackson parse explosion)
        if (body.trim().startsWith("<")) {
            String snippet = body.substring(0, Math.min(120, body.length()));
            throw new IOException("Server returned HTML (possible redirect, login, Cloudflare challenge or error page) instead of OpenAPI. Snippet: " + snippet);
        }

        return body;
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
