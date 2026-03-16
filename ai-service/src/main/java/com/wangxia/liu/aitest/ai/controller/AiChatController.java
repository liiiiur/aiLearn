package com.wangxia.liu.aitest.ai.controller;

import com.wangxia.liu.aitest.ai.dto.ChatRequest;
import com.wangxia.liu.aitest.ai.dto.ChatResponse;
import com.wangxia.liu.aitest.ai.service.AiChatService;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * AI 对话接口控制器。
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * 构造控制器。
     *
     * @param aiChatService 对话服务
     */
    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * 通过 HTTP 调用对话接口。
     *
     * @param request 对话请求
     * @return 对话响应
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.prompt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "输入内容不能为空");
        }
        String content = aiChatService.chat(request.prompt());
        return new ChatResponse(content);
    }

    /**
     * 通过 LangChain4j 调用对话接口。
     *
     * @param request 对话请求
     * @return 对话响应
     */
    @PostMapping("/langChat")
    public ChatResponse langChat(@RequestBody ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.prompt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "输入内容不能为空");
        }
        String content = aiChatService.langChat(request.prompt());
        return new ChatResponse(content);
    }
}
