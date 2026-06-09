package com.soulmate.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson ObjectMapper 配置
 * Spring Boot 4.0 默认使用 Jackson 3.x (tools.jackson)，此处手动注册 Jackson 2.x ObjectMapper
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
