package com.soulmate.service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;


/**
 * PgVector 向量数据库配置
 * 使用 PostgreSQL pgvector 扩展存储向量数据
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.pgvector.table-name:memory_vectors}")
    private String tableName;

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        log.info("初始化 PgVector 向量数据库, 表名: {}", tableName);

        PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(tableName)
                .initializeSchema(true)
                .indexType(org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW)
                .distanceType(org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .build();

        log.info("PgVector 向量数据库初始化成功");
        return vectorStore;
    }
}
