package com.wangxia.liu.aitest.ai.dto;

/**
 * 对话响应参数。
 */
public record ChatResponse(
        /**
         * 模型返回内容。
         */
        String content
) {
}
