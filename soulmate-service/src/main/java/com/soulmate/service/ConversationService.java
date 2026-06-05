package com.soulmate.service;

import com.soulmate.domain.entity.Conversation;
import com.soulmate.domain.entity.Message;
import com.soulmate.domain.dto.ChatRequest;
import com.soulmate.domain.dto.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 对话服务
 */
public interface ConversationService {

    /**
     * 创建或获取会话
     */
    Conversation getOrCreateConversation(Long userId, Long companionId);

    /**
     * 获取用户的会话列表
     */
    List<Conversation> getUserConversations(Long userId);

    /**
     * 获取历史消息（分页）
     */
    List<Message> getHistoryMessages(Long conversationId, int page, int size);

    /**
     * 发送消息并获取AI回复（SSE流式）
     */
    Flux<ChatResponse> sendMessage(Long userId, ChatRequest request);

    /**
     * 获取普通（非流式）AI回复
     */
    Message sendMessageSync(Long userId, ChatRequest request);
}
