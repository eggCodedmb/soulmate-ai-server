package com.soulmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SoulMate AI 启动入口
 */
@SpringBootApplication(exclude = {
        org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration.class,
        org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration.class
})
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class SoulMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoulMateApplication.class, args);
    }
}
