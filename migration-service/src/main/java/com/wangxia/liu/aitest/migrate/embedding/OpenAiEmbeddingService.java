package com.wangxia.liu.aitest.migrate.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.wangxia.liu.aitest.config.AiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiEmbeddingService implements EmbeddingService {

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public OpenAiEmbeddingService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new IllegalStateException("Please configure ai.api-key first.");
        }
        if (!StringUtils.hasText(aiProperties.getEmbeddingModel())) {
            throw new IllegalStateException("Please configure ai.embedding-model first.");
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiProperties.getEmbeddingModel());
        requestBody.put("input", texts);

        JsonNode response = restClient.post()
                .uri("/v1/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("Embedding response is empty.");
        }

        JsonNode data = response.path("data");
        if (!data.isArray()) {
            throw new IllegalStateException("Cannot parse embedding response data.");
        }

        List<List<Float>> vectors = new ArrayList<>(data.size());
        for (JsonNode item : data) {
            JsonNode embedding = item.path("embedding");
            if (!embedding.isArray()) {
                throw new IllegalStateException("Embedding item is invalid.");
            }
            List<Float> vector = new ArrayList<>(embedding.size());
            for (JsonNode value : embedding) {
                vector.add((float) value.asDouble());
            }
            vectors.add(vector);
        }

        if (vectors.size() != texts.size()) {
            throw new IllegalStateException("Embedding count mismatch: expected " + texts.size() + " but got " + vectors.size());
        }
        return vectors;
    }
}
