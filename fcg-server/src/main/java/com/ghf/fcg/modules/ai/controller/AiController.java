package com.ghf.fcg.modules.ai.controller;

import com.ghf.fcg.modules.ai.service.AiService;
import com.ghf.fcg.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI智能助手", description = "AI大模型调用接口")
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    @Operation(summary = "AI对话")
    public Result<String> chat(@RequestBody ChatRequest request) {
        String response = aiService.chat(request.getSystemPrompt(), request.getUserPrompt());
        return Result.success(response);
    }

    // ========== DTO ==========

    public static class ChatRequest {
        private String systemPrompt;
        private String userPrompt;

        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public String getUserPrompt() { return userPrompt; }
        public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }
    }
}
