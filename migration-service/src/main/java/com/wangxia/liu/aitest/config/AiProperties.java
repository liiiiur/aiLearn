package com.wangxia.liu.aitest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模型访问相关配置。
 */
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private String baseUrl = "https://api.openai.com";
    private String apiKey;
    private String model = "gpt-4o-mini";
    private String embeddingModel = "text-embedding-3-small";

    /**
     * 获取 AI 服务基础地址。
     *
     * @return 基础地址
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 设置 AI 服务基础地址。
     *
     * @param baseUrl 基础地址
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * 获取 API Key。
     *
     * @return API Key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * 设置 API Key。
     *
     * @param apiKey API Key
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 获取聊天模型名称。
     *
     * @return 模型名称
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置聊天模型名称。
     *
     * @param model 模型名称
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 获取向量模型名称。
     *
     * @return 向量模型名称
     */
    public String getEmbeddingModel() {
        return embeddingModel;
    }

    /**
     * 设置向量模型名称。
     *
     * @param embeddingModel 向量模型名称
     */
    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
}
