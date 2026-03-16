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

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.prompt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt cannot be blank");
        }
        String content = aiChatService.chat(request.prompt());
        return new ChatResponse(content);
    }

    @PostMapping("/langChat")
    public ChatResponse langChat(@RequestBody ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.prompt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt cannot be blank");
        }
        String content = aiChatService.langChat(request.prompt());
        return new ChatResponse(content);
    }
}
