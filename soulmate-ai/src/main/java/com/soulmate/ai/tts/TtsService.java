package com.soulmate.ai.tts;

import com.soulmate.common.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * 语音合成服务 - 基于小米 mimo-v2.5-tts
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService {

    private final AiProperties aiProperties;

    /**
     * 文本转语音
     *
     * @param text    需要朗读的文本
     * @param voiceId 音色标识
     * @return 合成的音频二进制数据 (WAV 格式)
     */
    @SuppressWarnings("unchecked")
    public byte[] generateTts(String text, String voiceId) {
        try {
            if (!aiProperties.getTts().isEnabled()) {
                log.warn("语音合成功能未启用");
                return new byte[0];
            }

            String apiKey = aiProperties.getApiKey();
            String model = aiProperties.getTts().getModel();
            String ttsBaseUrl = aiProperties.getTts().getBaseUrl();
            if (ttsBaseUrl == null || ttsBaseUrl.isBlank()) {
                ttsBaseUrl = aiProperties.getBaseUrl();
            }

            log.info("开始语音合成: textLength={}, voiceId={}, model={}", text.length(), voiceId, model);

            // 1. 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", "用温柔的声音朗读"),
                    Map.of("role", "assistant", "content", text)
            ));
            requestBody.put("audio", Map.of(
                    "format", "wav",
                    "voice", voiceId != null ? voiceId : "mimo_default"
            ));
            requestBody.put("stream", false);

            // 2. 调用 TTS 接口
            RestClient restClient = RestClient.create();
            Map<String, Object> response = restClient.post()
                    .uri(ttsBaseUrl + "/chat/completions")
                    .header("api-key", apiKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            // 3. 解析响应获取 Base64 音频并解码
            if (response == null) {
                log.error("TTS 响应为空");
                return new byte[0];
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("TTS 响应无 choices: {}", response);
                return new byte[0];
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                log.error("TTS 响应无 message: {}", choices.get(0));
                return new byte[0];
            }

            Map<String, Object> audioMap = (Map<String, Object>) message.get("audio");
            if (audioMap == null) {
                log.error("TTS 响应无 audio 数据: {}", message);
                return new byte[0];
            }

            String base64Data = (String) audioMap.get("data");
            if (base64Data == null || base64Data.isEmpty()) {
                log.error("TTS 响应音频 Base64 为空");
                return new byte[0];
            }

            log.info("语音合成成功，字节数: {}", base64Data.length());
            return Base64.getDecoder().decode(base64Data);

        } catch (Exception e) {
            log.error("语音合成失败: text={}", text, e);
            return new byte[0];
        }
    }
}
