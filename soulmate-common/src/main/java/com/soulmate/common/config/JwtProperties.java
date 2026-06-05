package com.soulmate.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "soulmate.jwt")
public class JwtProperties {

    /** 密钥（至少32字节） */
    private String secret = "soulmate-ai-default-secret-key-32bytes!";

    /** Token 有效期（毫秒），默认7天 */
    private long expireMs = 7 * 24 * 60 * 60 * 1000L;
}
