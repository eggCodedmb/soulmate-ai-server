package com.soulmate.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 模型配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "soulmate.ai")
public class AiProperties {

    /** 默认模型编码 */
    private String defaultModel = "mimo-v2.5-pro";

    /** 通话模型编码 */
    private String callModel = "mimo-v2.5-flash";

    /** 系统提示词模板路径 */
    private String systemPromptTemplate = "classpath:prompts/system-prompt.st";

    /** API 基础地址 */
    private String baseUrl = "https://token-plan-sgp.xiaomimimo.com/v1";

    /** API Key */
    private String apiKey;

    /** ASR 语音识别配置 */
    private Asr asr = new Asr();

    /** TTS 语音合成配置 */
    private Tts tts = new Tts();

    @Data
    public static class Asr {
        /** 是否启用语音识别 */
        private boolean enabled = true;

        /** ASR 模型名称 */
        private String model = "mimo-v2.5-asr";

        /** ASR API 基础地址 */
        private String baseUrl;

        /** 最大音频文件大小（MB） */
        private int maxSizeMb = 25;
    }

    @Data
    public static class Tts {
        /** 是否启用语音合成 */
        private boolean enabled = true;

        /** TTS 模型名称 */
        private String model = "mimo-v2.5-tts";

        /** TTS API 基础地址 */
        private String baseUrl;
    }
}
