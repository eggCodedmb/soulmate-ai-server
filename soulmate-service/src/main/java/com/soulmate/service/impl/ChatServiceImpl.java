package com.soulmate.service.impl;

import com.soulmate.domain.entity.Companion;
import com.soulmate.domain.entity.Conversation;
import com.soulmate.service.ChatService;
import com.soulmate.domain.dto.ChatRequest;
import com.soulmate.domain.dto.ChatResponse;
import com.soulmate.ai.mcp.TimeToolService;
import com.soulmate.ai.mcp.WeatherToolService;
import com.soulmate.common.config.AiProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 聊天服务实现
 * 使用 Spring AI ChatClient 调用 LLM（OpenAI 兼容 API）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final ChatModel chatModel;
    private final PromptBuilder promptBuilder;
    private final WeatherToolService weatherToolService;
    private final TimeToolService timeToolService;
    private final DynamicLlmService dynamicLlmService;
    private final AiProperties aiProperties;

    /** 核心聊天客户端（集成所有工具） */
    private ChatClient chatClient;

    @PostConstruct
    public void init() {
        chatClient = chatClientBuilder
                .defaultTools(weatherToolService, timeToolService)
                .build();
    }

    @Override
    public Flux<ChatResponse> streamChat(Long userId, Conversation conversation,
                                          Companion companion, String userMessage,
                                          ChatRequest request) {
        try {
            boolean isVoiceCall = request != null && "voice_call".equalsIgnoreCase(request.getSceneMode());
            List<Message> messages = promptBuilder.buildMessages(userId, conversation, companion, userMessage, isVoiceCall);
            log.info("聊天请求: userId={}, llmType={}, model={}",
                    userId,
                    request != null && request.getLlmProviderType() != null ? request.getLlmProviderType() : "system",
                    request != null && request.getLlmModel() != null ? request.getLlmModel() : "default");

            // 1. 先保存用户消息到上下文（如果是开场白，跳过用户指令的保存，只保存稍后AI生成的开场白）
            if (!"[GREETING]".equals(userMessage)) {
                promptBuilder.saveContext(conversation.getId(), "user", userMessage, conversation.getContextWindow());
            }

            ChatClient client = resolveDynamicClient(request);
            AtomicInteger chunkCount = new AtomicInteger(0);
            StringBuilder fullContent = new StringBuilder();

            // Spring AI 2.0.0: stream() 只在 tool call 时缓冲，普通文本流逐 chunk 发射
            return client.prompt()
                    .messages(messages)
                    .stream()
                    .chatResponse()
                    .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                    .map(response -> {
                        String content = "";
                        if (response.getResult() != null && response.getResult().getOutput() != null) {
                            content = response.getResult().getOutput().getText();
                        }

                        if (content == null || content.isEmpty()) {
                            return ChatResponse.builder()
                                    .conversationId(conversation.getId())
                                    .content("")
                                    .done(false)
                                    .build();
                        }

                        fullContent.append(content);

                        int count = chunkCount.incrementAndGet();
                        log.debug("SSE Chunk #{}: size={}, content=[{}]", count, content.length(),
                                content.replace("\n", "\\n"));

                        return ChatResponse.builder()
                                .conversationId(conversation.getId())
                                .content(content)
                                .done(false)
                                .build();
                    })
                    .doOnComplete(() -> {
                        log.info("SSE流完成: userId={}, conversationId={}, totalChunks={}",
                                userId, conversation.getId(), chunkCount.get());
                        if (fullContent.length() > 0) {
                            promptBuilder.saveContext(conversation.getId(), "assistant", fullContent.toString(), conversation.getContextWindow());
                        }
                    })
                    .onErrorResume(e -> {
                        String errorMsg = "AI服务暂时不可用，请稍后再试";
                        if (e instanceof java.util.concurrent.TimeoutException) {
                            errorMsg = "AI响应超时，请检查模型服务是否正常运行";
                        } else if (e.getMessage() != null && e.getMessage().contains("quota exhausted")) {
                            errorMsg = "AI额度已用尽，请检查账户余额或更换API Key";
                        }

                        log.error("AI流式响应异常: userId={}, conversationId={}, errorType={}, msg={}, chunksBeforeError={}",
                                userId, conversation.getId(), e.getClass().getSimpleName(), e.getMessage(), chunkCount.get());
                        return Flux.just(ChatResponse.builder()
                                .conversationId(conversation.getId())
                                .error(errorMsg)
                                .done(true)
                                .build());
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
                           Companion companion, String userMessage,
                           ChatRequest request) {
        try {
            boolean isVoiceCall = request != null && "voice_call".equalsIgnoreCase(request.getSceneMode());
            List<Message> messages = promptBuilder.buildMessages(userId, conversation, companion, userMessage, isVoiceCall);
            ChatClient client = resolveDynamicClient(request);
            log.info("聊天请求: userId={}, llmType={}, model={}",
                    userId,
                    request != null && request.getLlmProviderType() != null ? request.getLlmProviderType() : "system",
                    request != null && request.getLlmModel() != null ? request.getLlmModel() : "default");

            promptBuilder.saveContext(conversation.getId(), "user", userMessage, conversation.getContextWindow());

            org.springframework.ai.chat.model.ChatResponse response = client.prompt()
                    .messages(messages)
                    .call()
                    .chatResponse();

            if (response.getResult() != null && response.getResult().getOutput() != null) {
                String content = response.getResult().getOutput().getText();
                if (content != null) {
                    promptBuilder.saveContext(conversation.getId(), "assistant", content, conversation.getContextWindow());
                }
                return content;
            }
            return "抱歉，我暂时无法回复。";

        } catch (Exception e) {
            log.error("AI同步聊天异常: userId={}, conversationId={}", userId, conversation.getId(), e);
            if (e.getMessage() != null && e.getMessage().contains("quota exhausted")) {
                return "抱歉，AI额度已用尽，请检查账户余额或更换API Key。";
            }
            return "抱歉，AI服务暂时不可用，请稍后再试。";
        }
    }

    /**
     * 根据请求动态解析 ChatClient
     */
    private ChatClient resolveDynamicClient(ChatRequest request) {
        ChatModel model = dynamicLlmService.resolveChatModel(request, chatModel);
        if (model == chatModel) {
            return chatClient;
        }
        return ChatClient.builder(model)
                .defaultTools(weatherToolService, timeToolService)
                .build();
    }
}
