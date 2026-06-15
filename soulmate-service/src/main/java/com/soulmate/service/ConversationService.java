package com.soulmate.service;

import com.soulmate.common.response.PageResult;
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
    PageResult<Message> getHistoryMessages(Long conversationId, int page, int size);

    /**
     * 发送消息并获取AI回复（SSE流式）
     */
    Flux<ChatResponse> sendMessage(Long userId, ChatRequest request);

    /**
     * 发送开场白指令并获取AI回复（SSE流式，不保存用户发送的 [GREETING] 消息）
     */
    Flux<ChatResponse> sendGreeting(Long userId, ChatRequest request);

    /**
     * 获取普通（非流式）AI回复
     */
    Message sendMessageSync(Long userId, ChatRequest request);

    /**
     * 删除单条消息
     */
    void deleteMessage(Long userId, Long messageId);
}

