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

/**
 * AI 对话服务。
 */
@Service
public class AiChatService {

    private final AiProperties aiProperties;
    private final RestClient restClient;

    /**
     * 构建对话服务。
     *
     * @param aiProperties 配置参数
     */
    public AiChatService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 通过 HTTP 调用对话接口。
     *
     * @param prompt 用户输入
     * @return 模型返回内容
     */
    public String chat(String prompt) {
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new IllegalStateException("请先配置 ai.api-key。");
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
            throw new IllegalStateException("AI 返回为空。");
        }

        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull() || !content.isTextual()) {
            throw new IllegalStateException("无法解析 AI 返回内容。");
        }
        return content.asText();
    }

    /**
     * 通过 LangChain4j 调用对话接口。
     *
     * @param prompt 用户输入
     * @return 模型返回内容
     */
    public String langChat(String prompt) {
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new IllegalStateException("请先配置 ai.api-key。");
        }
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(aiProperties.getApiKey())
                .baseUrl(aiProperties.getBaseUrl())
                .modelName(aiProperties.getModel())
                .build();

        return model.generate(prompt);
    }
}
