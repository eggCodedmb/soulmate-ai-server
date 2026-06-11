package com.soulmate.service.impl;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.soulmate.domain.dto.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态 LLM 模型解析服务
 * 根据客户端传入的 LLM 配置，动态构建 ChatModel / ChatClient
 */
@Slf4j
@Service
public class DynamicLlmService {

    private final ConcurrentHashMap<String, ChatModel> modelCache = new ConcurrentHashMap<>();

    /**
     * 根据请求解析 ChatClient
     * - providerType 为 null 或 "system" → 返回 fallback（系统默认）
     * - providerType 为 "openai" → 动态构建并缓存
     */
    public ChatClient resolveChatClient(ChatRequest request, ChatClient fallback) {
        if (request == null
                || request.getLlmProviderType() == null
                || "system".equals(request.getLlmProviderType())) {
            return fallback;
        }

        if ("openai".equals(request.getLlmProviderType())) {
            String baseUrl = request.getLlmBaseUrl();
            if (baseUrl == null || baseUrl.isBlank()) {
                log.warn("llmProviderType=openai 但 llmBaseUrl 为空，回退到系统默认");
                return fallback;
            }
            ChatModel model = getOrCreateModel(baseUrl, request.getLlmApiKey(), request.getLlmModel());
            return ChatClient.builder(model).build();
        }

        log.warn("未知的 llmProviderType: {}，回退到系统默认", request.getLlmProviderType());
        return fallback;
    }

    private ChatModel getOrCreateModel(String baseUrl, String apiKey, String model) {
        String cacheKey = baseUrl + "::" + (model != null ? model : "default");

        return modelCache.computeIfAbsent(cacheKey, k -> {
            String effectiveKey = (apiKey != null && !apiKey.isEmpty()) ? apiKey : "ollama";
            String effectiveModel = (model != null && !model.isBlank()) ? model : "gpt-4o";
            log.info("创建动态 ChatModel: baseUrl={}, model={}", baseUrl, effectiveModel);

            OpenAIClient openAiClient = OpenAIOkHttpClient.builder()
                    .baseUrl(baseUrl)
                    .apiKey(effectiveKey)
                    .build();

            OpenAIClientAsync openAiClientAsync = OpenAIOkHttpClientAsync.builder()
                    .baseUrl(baseUrl)
                    .apiKey(effectiveKey)
                    .build();

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(effectiveModel)
                    .temperature(0.7)
                    .build();

            return OpenAiChatModel.builder()
                    .openAiClient(openAiClient)
                    .openAiClientAsync(openAiClientAsync)
                    .options(options)
                    .build();
        });
    }
}
