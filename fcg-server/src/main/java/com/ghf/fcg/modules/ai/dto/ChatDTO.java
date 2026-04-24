package com.ghf.fcg.modules.ai.dto;

/**
 * AI 对话请求参数
 */
public class ChatDTO {

    private String systemPrompt;
    private String userPrompt;

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }
}
