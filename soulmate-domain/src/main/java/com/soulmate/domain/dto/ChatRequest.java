package com.soulmate.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 聊天请求
 */
@Data
public class ChatRequest {

    /** 会话ID */
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    /** 伴侣ID */
    @NotNull(message = "伴侣ID不能为空")
    private Long companionId;

    /** 消息内容 */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** 内容类型：text/voice/image，默认 text */
    private String contentType = "text";

    /** 场景模式（可选，切换场景时传入） */
    private String sceneMode;

    // ===== LLM 模型切换（可选，不传则使用系统默认模型） =====

    /** LLM 提供商类型：system（默认）| openai（OpenAI 协议，含 Ollama） */
    private String llmProviderType;

    /** LLM Base URL（如 https://api.deepseek.com/v1 或 http://localhost:11434/v1） */
    private String llmBaseUrl;

    /** LLM API Key（Ollama 等本地模型可不传） */
    private String llmApiKey;

    /** LLM 模型名称（如 deepseek-chat、qwen2.5:7b） */
    private String llmModel;
}
