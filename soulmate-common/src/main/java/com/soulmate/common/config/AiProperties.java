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

    /** API Key */
    private String apiKey;

    /** API 基础地址 */
    private String baseUrl = "https://token-plan-sgp.xiaomimimo.com/v1";

    /** 温度 */
    private double temperature = 0.7;

    /** 最大 token */
    private int maxTokens = 2048;

    /** 系统提示词模板路径 */
    private String systemPromptTemplate = "classpath:prompts/system-prompt.st";
}
