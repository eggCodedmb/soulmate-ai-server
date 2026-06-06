package com.soulmate.service.impl;

import com.soulmate.common.config.AiProperties;
import com.soulmate.service.impl.PromptBuilder;
import com.soulmate.domain.entity.Companion;
import com.soulmate.domain.entity.Conversation;
import com.soulmate.service.ChatService;
import com.soulmate.domain.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 聊天服务实现
 * 使用 Spring AI ChatClient 调用 mimo-v2.5-pro（OpenAI 兼容 API）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final PromptBuilder promptBuilder;
    private final AiProperties aiProperties;

    @Override
    public Flux<ChatResponse> streamChat(Long userId, Conversation conversation,
                                          Companion companion, String userMessage) {
        try {
            List<Message> messages = promptBuilder.buildMessages(userId, conversation, companion, userMessage);

            ChatClient chatClient = chatClientBuilder.build();

            return chatClient.prompt()
                    .messages(messages)
                    .stream()
                    .chatResponse()
                    .map(response -> {
                        String content = "";
                        if (response.getResult() != null && response.getResult().getOutput() != null) {
                            content = response.getResult().getOutput().getContent();
                        }
                        return ChatResponse.builder()
                                .conversationId(conversation.getId())
                                .content(content)
                                .done(false)
                                .build();
                    })
                    .concatWithValues(ChatResponse.builder()
                            .conversationId(conversation.getId())
                            .content("")
                            .done(true)
                            .build());

        } catch (Exception e) {
            log.error("AI流式聊天异常: userId={}, conversationId={}", userId, conversation.getId(), e);
            return Flux.just(ChatResponse.builder()
                    .conversationId(conversation.getId())
                    .error("AI服务暂时不可用，请稍后再试")
                    .done(true)
                    .build());
        }
    }

    @Override
    public String chatSync(Long userId, Conversation conversation,
                           Companion companion, String userMessage) {
        try {
            List<Message> messages = promptBuilder.buildMessages(userId, conversation, companion, userMessage);

            ChatClient chatClient = chatClientBuilder.build();

            org.springframework.ai.chat.model.ChatResponse response = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .chatResponse();

            if (response.getResult() != null && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getContent();
            }
            return "抱歉，我暂时无法回复。";

        } catch (Exception e) {
            log.error("AI同步聊天异常: userId={}, conversationId={}", userId, conversation.getId(), e);
            return "抱歉，AI服务暂时不可用，请稍后再试。";
        }
    }
}
