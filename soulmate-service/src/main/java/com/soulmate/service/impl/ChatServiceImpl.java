package com.soulmate.service.impl;

import com.soulmate.service.impl.PromptBuilder;
import com.soulmate.domain.entity.Companion;
import com.soulmate.domain.entity.Conversation;
import com.soulmate.service.ChatService;
import com.soulmate.domain.dto.ChatResponse;
import com.soulmate.ai.mcp.TimeToolService;
import com.soulmate.ai.mcp.WeatherToolService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;

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
    private final WeatherToolService weatherToolService;
    private final TimeToolService timeToolService;

    /** 普通聊天客户端（不带工具） */
    private ChatClient chatClient;
    /** 带天气工具的聊天客户端 */
    private ChatClient weatherChatClient;
    /** 带时间工具的聊天客户端 */
    private ChatClient timeChatClient;

    /** 天气相关关键词，用户消息包含其中任意一个才会注册天气工具 */
    private static final Set<String> WEATHER_KEYWORDS = Set.of(
            "天气", "气温", "温度", "下雨", "下雪", "晴天", "阴天", "多云",
            "台风", "暴雨", "寒潮", "降温", "升温", "湿度", "风力",
            "weather", "temperature", "rain", "snow"
    );

    /** 时间相关关键词，用户消息包含其中任意一个才会注册时间工具 */
    private static final Set<String> TIME_KEYWORDS = Set.of(
            "几点", "时间", "日期", "今天", "星期", "几号",
            "what time", "what date", "time", "today"
    );

    @PostConstruct
    public void init() {
        chatClient = chatClientBuilder.build();
        weatherChatClient = chatClientBuilder.build().mutate()
                .defaultTools(weatherToolService)
                .build();
        timeChatClient = chatClientBuilder.build().mutate()
                .defaultTools(timeToolService)
                .build();
    }

    @Override
    public Flux<ChatResponse> streamChat(Long userId, Conversation conversation,
                                          Companion companion, String userMessage) {
        try {
            List<Message> messages = promptBuilder.buildMessages(userId, conversation, companion, userMessage);
            ChatClient client = resolveClient(userMessage);
            log.info("聊天请求: userId={}, client={}", client == chatClient ? "default" : (client == weatherChatClient ? "weather" : "time"));

            return client.prompt()
                    .messages(messages)
                    .stream()
                    .chatResponse()
                    .map(response -> {
                        String content = "";
                        if (response.getResult() != null && response.getResult().getOutput() != null) {
                            content = response.getResult().getOutput().getText();
                            // 调试：检查是否有 tool call 泄漏到文本流
                            if (content != null && (content.contains("<tool_call>") || content.contains("<function="))) {
                                log.warn("检测到tool call文本泄漏: {}", content);
                            }
                        }
                        return ChatResponse.builder()
                                .conversationId(conversation.getId())
                                .content(content)
                                .done(false)
                                .build();
                    })
                    .onErrorResume(e -> {
                        log.error("AI流式响应异常: userId={}, conversationId={}", userId, conversation.getId(), e);
                        return Flux.just(ChatResponse.builder()
                                .conversationId(conversation.getId())
                                .error("AI服务暂时不可用，请稍后再试")
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
                           Companion companion, String userMessage) {
        try {
            List<Message> messages = promptBuilder.buildMessages(userId, conversation, companion, userMessage);
            ChatClient client = resolveClient(userMessage);
            log.info("聊天请求: userId={}, client={}", client == chatClient ? "default" : (client == weatherChatClient ? "weather" : "time"));

            org.springframework.ai.chat.model.ChatResponse response = client.prompt()
                    .messages(messages)
                    .call()
                    .chatResponse();

            if (response.getResult() != null && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getText();
            }
            return "抱歉，我暂时无法回复。";

        } catch (Exception e) {
            log.error("AI同步聊天异常: userId={}, conversationId={}", userId, conversation.getId(), e);
            return "抱歉，AI服务暂时不可用，请稍后再试。";
        }
    }

    /**
     * 根据用户消息选择合适的 ChatClient
     */
    private ChatClient resolveClient(String message) {
        if (message == null) {
            return chatClient;
        }
        String lower = message.toLowerCase();
        if (WEATHER_KEYWORDS.stream().anyMatch(lower::contains)) {
            return weatherChatClient;
        }
        if (TIME_KEYWORDS.stream().anyMatch(lower::contains)) {
            return timeChatClient;
        }
        return chatClient;
    }
}
