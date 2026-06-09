package com.soulmate.ai.asr;

import com.soulmate.common.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * 语音识别服务 - 基于小米 mimo-v2.5-asr
 * 通过 chat completions 接口调用 ASR 能力
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsrService {

    private final AiProperties aiProperties;

    /**
     * 语音转文字
     *
     * @param audioData 音频二进制数据
     * @param fileName  文件名（用于推断 MIME 类型）
     * @return 识别出的文字，失败返回 null
     */
    @SuppressWarnings("unchecked")
    public String transcribe(byte[] audioData, String fileName) {
        try {
            if (!aiProperties.getAsr().isEnabled()) {
                log.warn("语音识别功能未启用");
                return null;
            }

            String apiKey = aiProperties.getApiKey();
            String model = aiProperties.getAsr().getModel();
            // 如果 ASR 没有单独配置 baseUrl，则使用主 AI 的 baseUrl
            String asrBaseUrl = aiProperties.getAsr().getBaseUrl();
            if (asrBaseUrl == null || asrBaseUrl.isBlank()) {
                asrBaseUrl = aiProperties.getBaseUrl();
            }

            log.info("开始语音识别: fileName={}, size={}KB, model={}", fileName, audioData.length / 1024, model);

            // 1. Base64 编码音频
            String base64Audio = Base64.getEncoder().encodeToString(audioData);
            String mimeType = getMimeType(fileName);

            // 2. 构建请求体
            Map<String, Object> requestBody = buildRequestBody(base64Audio, mimeType, model);

            // 3. 调用 ASR 接口
            RestClient restClient = RestClient.create();
            Map<String, Object> response = restClient.post()
                    .uri(asrBaseUrl + "/chat/completions")
                    .header("api-key", apiKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            // 4. 解析响应
            return parseResponse(response, fileName);

        } catch (Exception e) {
            log.error("语音识别失败: fileName={}", fileName, e);
            return null;
        }
    }

    /**
     * 构建 ASR 请求体
     */
    private Map<String, Object> buildRequestBody(String base64Audio, String mimeType, String model) {
        // audio content
        Map<String, Object> audioData = new HashMap<>();
        audioData.put("data", "data:" + mimeType + ";base64," + base64Audio);

        Map<String, Object> inputAudio = new HashMap<>();
        inputAudio.put("type", "input_audio");
        inputAudio.put("input_audio", audioData);

        // message
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", List.of(inputAudio));

        // asr_options
        Map<String, Object> asrOptions = new HashMap<>();
        asrOptions.put("language", "auto");

        // request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(message));
        requestBody.put("asr_options", asrOptions);
        requestBody.put("stream", false);

        return requestBody;
    }

    /**
     * 解析 ASR 响应
     */
    @SuppressWarnings("unchecked")
    private String parseResponse(Map<String, Object> response, String fileName) {
        if (response == null) {
            log.error("ASR 响应为空");
            return null;
        }

        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("ASR 响应无 choices: {}", response);
                return null;
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                log.error("ASR 响应无 message: {}", choices.get(0));
                return null;
            }

            String text = (String) message.get("content");
            if (text == null || text.isBlank()) {
                log.error("ASR 响应 content 为空: {}", message);
                return null;
            }

            log.info("语音识别成功: fileName={}, text={}", fileName,
                    text.length() > 100 ? text.substring(0, 100) + "..." : text);
            return text.trim();

        } catch (Exception e) {
            log.error("解析 ASR 响应失败: {}", response, e);
            return null;
        }
    }

    /**
     * 根据文件名推断 MIME 类型
     */
    private String getMimeType(String fileName) {
        if (fileName == null) {
            return "audio/wav";
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (lower.endsWith(".m4a")) {
            return "audio/mp4";
        } else if (lower.endsWith(".webm")) {
            return "audio/webm";
        } else if (lower.endsWith(".ogg")) {
            return "audio/ogg";
        } else if (lower.endsWith(".flac")) {
            return "audio/flac";
        }
        return "audio/wav";
    }
}
