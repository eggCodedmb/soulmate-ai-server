package com.soulmate.service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 向量库配置 (轻量级本地文件存储)
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    @Value("${soulmate.file.base-dir}")
    private String baseDir;

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        String vectorDbPath = baseDir + File.separator + "vector_store.json";
        File file = new File(vectorDbPath);
        
        SimpleVectorStore vectorStore = new SimpleVectorStore(embeddingModel);
        
        if (file.exists()) {
            try {
                vectorStore.load(file);
                log.info("本地向量库加载成功: {}", vectorDbPath);
            } catch (Exception e) {
                log.warn("本地向量库加载失败，初始化新库: {}", e.getMessage());
            }
        } else {
            log.info("初始化新本地向量库: {}", vectorDbPath);
        }
        
        return vectorStore;
    }
}
