package com.wangxia.liu.aitest.ai.dto;

/**
 * 对话请求参数。
 */
public record ChatRequest(
        /**
         * 用户输入内容。
         */
        String prompt
) {
}
