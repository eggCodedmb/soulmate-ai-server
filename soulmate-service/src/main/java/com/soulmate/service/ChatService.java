package com.soulmate.service;

import com.soulmate.domain.entity.Companion;
import com.soulmate.domain.entity.Conversation;
import com.soulmate.domain.dto.ChatRequest;
import com.soulmate.domain.dto.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * AI 聊天服务接口
 */
public interface ChatService {

    /**
     * 流式聊天（SSE）
     * @param request 透传 ChatRequest，用于获取 LLM 模型配置
     */
    Flux<ChatResponse> streamChat(Long userId, Conversation conversation,
                                  Companion companion, String userMessage,
                                  ChatRequest request);

    /**
     * 同步聊天（非流式）
     * @param request 透传 ChatRequest，用于获取 LLM 模型配置
     */
    String chatSync(Long userId, Conversation conversation,
                    Companion companion, String userMessage,
                    ChatRequest request);
}
