package com.soulmate.service.impl;

import com.soulmate.domain.dto.ChatRequest;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.soulmate.common.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态 LLM 模型解析服务
 * 根据客户端传入的 LLM 配置，动态构建 ChatModel
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicLlmService {

    private final AiProperties aiProperties;
    private final ConcurrentHashMap<String, ChatModel> modelCache = new ConcurrentHashMap<>();

    /**
     * 根据请求解析 ChatModel
     * - providerType 为 null 或 "system" → 返回 fallback（系统默认）
     * - providerType 为 "openai" → 动态构建并缓存
     */
    public ChatModel resolveChatModel(ChatRequest request, ChatModel fallback) {
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
            return getOrCreateModel(baseUrl, request.getLlmApiKey(), request.getLlmModel());
        }

        log.warn("未知的 llmProviderType: {}，回退到系统默认", request.getLlmProviderType());
        return fallback;
    }

    private ChatModel getOrCreateModel(String baseUrl, String apiKey, String model) {
        String cacheKey = baseUrl + "::" + (model != null ? model : "default");

        return modelCache.computeIfAbsent(cacheKey, k -> {
            String effectiveModel = (model != null && !model.isBlank()) ? model : "gpt-4o";

            // 检测是否为 Ollama URL
            if (isOllamaUrl(baseUrl)) {
                log.info("创建动态 Ollama ChatModel: baseUrl={}, model={}", baseUrl, effectiveModel);
                // 移除 /v1 后缀，Ollama 内部 API 使用基础 URL
                String nativeUrl = baseUrl.replaceAll("/v1/?$", "");
                
                OllamaApi ollamaApi = OllamaApi.builder()
                        .baseUrl(nativeUrl)
                        .build();

                return OllamaChatModel.builder()
                        .ollamaApi(ollamaApi)
                        .defaultOptions(OllamaChatOptions.builder()
                                .model(effectiveModel)
                                .disableThinking() // 禁用 thinking 模式，解决推理模型 content 为空的问题
                                .temperature(0.7)
                                .numPredict(2048)
                                .build())
                        .build();
            }

            log.info("创建动态 OpenAI ChatModel: baseUrl={}, model={}", baseUrl, effectiveModel);
            String effectiveKey = (apiKey != null && !apiKey.isEmpty()) ? apiKey : "ollama";

            OpenAIClient openAiClient = OpenAIOkHttpClient.builder()
                    .baseUrl(baseUrl)
                    .apiKey(effectiveKey)
                    .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                    .build();

            OpenAIClientAsync openAiClientAsync = OpenAIOkHttpClientAsync.builder()
                    .baseUrl(baseUrl)
                    .apiKey(effectiveKey)
                    .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                    .build();

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(effectiveModel)
                    .temperature(0.7)
                    .maxTokens(2048)
                    .build();

            return OpenAiChatModel.builder()
                    .openAiClient(openAiClient)
                    .openAiClientAsync(openAiClientAsync)
                    .options(options)
                    .build();
        });
    }

    /**
     * 判断是否为 Ollama 服务地址
     * Ollama 默认端口 11434
     */
    public boolean isOllamaUrl(String baseUrl) {
        return baseUrl != null && baseUrl.contains("11434");
    }
}
