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

/**
 * 基于 OpenAI 兼容接口的向量化实现。
 */
@Service
public class OpenAiEmbeddingService implements EmbeddingService {

    private final AiProperties aiProperties;
    private final RestClient restClient;

    /**
     * 构造向量化服务。
     *
     * @param aiProperties 配置参数
     */
    public OpenAiEmbeddingService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 调用远程接口进行批量向量化。
     *
     * @param texts 文本列表
     * @return 向量列表
     */
    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new IllegalStateException("请先配置 ai.api-key。");
        }
        if (!StringUtils.hasText(aiProperties.getEmbeddingModel())) {
            throw new IllegalStateException("请先配置 ai.embedding-model。");
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
            throw new IllegalStateException("向量化返回为空。");
        }

        JsonNode data = response.path("data");
        if (!data.isArray()) {
            throw new IllegalStateException("无法解析向量化返回数据。");
        }

        List<List<Float>> vectors = new ArrayList<>(data.size());
        for (JsonNode item : data) {
            JsonNode embedding = item.path("embedding");
            if (!embedding.isArray()) {
                throw new IllegalStateException("向量化数据项无效。");
            }
            List<Float> vector = new ArrayList<>(embedding.size());
            for (JsonNode value : embedding) {
                vector.add((float) value.asDouble());
            }
            vectors.add(vector);
        }

        if (vectors.size() != texts.size()) {
            throw new IllegalStateException("向量化数量不匹配：期望 " + texts.size() + "，实际 " + vectors.size());
        }
        return vectors;
    }
}
