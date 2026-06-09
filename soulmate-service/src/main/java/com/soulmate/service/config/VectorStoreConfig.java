package com.soulmate.service.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.concurrent.TimeUnit;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 向量数据库配置
 * 启动时即建立连接，避免聊天时因连接延迟增加回复时间。
 * 若 Milvus 不可用，应用照常启动，向量检索降级为关键词搜索。
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.milvus.client.host}")
    private String host;

    @Value("${spring.ai.vectorstore.milvus.client.port}")
    private int port;

    @Value("${spring.ai.vectorstore.milvus.collection-name:memory_vectors}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.milvus.index-type:IVF_FLAT}")
    private String indexType;

    @Value("${spring.ai.vectorstore.milvus.metric-type:COSINE}")
    private String metricType;

    @Value("${spring.ai.vectorstore.milvus.initialize-schema:true}")
    private boolean initializeSchema;

    @Bean
    public MilvusServiceClient milvusClient() {
        log.info("启动时连接 Milvus 向量数据库: {}:{}", host, port);
        try {
            MilvusServiceClient client = new MilvusServiceClient(ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port)
                    .withConnectTimeout(5, TimeUnit.SECONDS)
                    .withKeepAliveTime(60, TimeUnit.SECONDS)
                    .build());
            log.info("Milvus 向量数据库连接成功");
            return client;
        } catch (Exception e) {
            log.warn("Milvus 启动时连接失败，向量检索将降级为关键词搜索: {}", e.getMessage());
            return null;
        }
    }

    @Bean
    public VectorStore vectorStore(MilvusServiceClient milvusClient, EmbeddingModel embeddingModel) {
        if (milvusClient == null) {
            log.warn("Milvus 客户端不可用，跳过 VectorStore 初始化");
            return null;
        }
        return MilvusVectorStore.builder(milvusClient, embeddingModel)
                .collectionName(collectionName)
                .indexType(IndexType.valueOf(indexType))
                .metricType(MetricType.valueOf(metricType))
                .initializeSchema(initializeSchema)
                .build();
    }
}
