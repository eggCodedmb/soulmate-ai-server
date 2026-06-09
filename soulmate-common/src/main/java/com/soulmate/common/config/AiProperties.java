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

    /** 系统提示词模板路径 */
    private String systemPromptTemplate = "classpath:prompts/system-prompt.st";
}
