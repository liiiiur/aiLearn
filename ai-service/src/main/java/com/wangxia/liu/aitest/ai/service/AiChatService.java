package com.wangxia.liu.aitest.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wangxia.liu.aitest.config.AiProperties;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public AiChatService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String chat(String prompt) {
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new IllegalStateException("Please configure ai.api-key first.");
        }

        Map<String, Object> requestBody = Map.of(
                "model", aiProperties.getModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        JsonNode response = restClient.post()
                .uri("/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("AI response is empty.");
        }

        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull() || !content.isTextual()) {
            throw new IllegalStateException("Cannot parse AI response content.");
        }
        return content.asText();
    }

    public String langChat(String prompt){
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new IllegalStateException("Please configure ai.api-key first.");
        }
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(aiProperties.getApiKey())
                .baseUrl(aiProperties.getBaseUrl())
                .modelName(aiProperties.getModel())
                .build();

        return model.generate(prompt);

    }
}
