package com.soulmate.service.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.CollectionSchemaParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.index.CreateIndexParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Milvus 向量数据库配置
 * 手动管理集合初始化，以绕过 Milvus Java SDK 与 Milvus Lite 之间的版本兼容性 Bug
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.milvus.client.host:127.0.0.1}")
    private String host;

    @Value("${spring.ai.vectorstore.milvus.client.port:19530}")
    private int port;

    @Value("${spring.ai.vectorstore.milvus.collection-name:memory_vectors}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.milvus.embedding-dimension:384}")
    private int dimension;

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
            log.info("Milvus 客户端创建成功");
            return client;
        } catch (Exception e) {
            log.error("Milvus 客户端创建失败: {}", e.getMessage());
            return null;
        }
    }

    @Bean
    public VectorStore vectorStore(MilvusServiceClient milvusClient, EmbeddingModel embeddingModel) {
        if (milvusClient == null) {
            log.warn("Milvus 客户端不可用，跳过 VectorStore 初始化");
            return null;
        }

        // 手动初始化 Schema，避开 Spring AI 内部调用 loadCollection 导致的 Bug
        initMilvusSchema(milvusClient);

        return MilvusVectorStore.builder(milvusClient, embeddingModel)
                .collectionName(collectionName)
                .iDFieldName("id") // 与手动创建的 schema 主键字段名一致
                .initializeSchema(false) // 禁用 Spring AI 的自动初始化
                .build();
    }

    public void initMilvusSchema(MilvusServiceClient client) {
        try {
            // 1. 检查集合是否存在
            var hasResp = client.hasCollection(HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
            
            if (hasResp.getException() != null) {
                log.error("检查集合是否存在时出错: {}", hasResp.getException().getMessage());
                return;
            }

            if (Boolean.TRUE.equals(hasResp.getData())) {
                log.info("Milvus 集合 {} 已存在，正在加载到内存", collectionName);
                client.loadCollection(LoadCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withSyncLoad(false) // 禁用同步加载，防止 SDK 内部 showCollections 检查时由于 percentage 数量不匹配抛出 IllegalResponseException
                        .build());
                return;
            }

            log.info("正在创建 Milvus 集合: {}", collectionName);

            // 2. 定义字段 (与 Spring AI MilvusVectorStore 要求的默认结构一致)
            FieldType idField = FieldType.newBuilder()
                    .withName("id")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(36)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();
            
            FieldType vectorField = FieldType.newBuilder()
                    .withName("embedding")
                    .withDataType(DataType.FloatVector)
                    .withDimension(dimension)
                    .build();

            FieldType contentField = FieldType.newBuilder()
                    .withName("content")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(65535)
                    .build();

            FieldType metadataField = FieldType.newBuilder()
                    .withName("metadata")
                    .withDataType(DataType.JSON)
                    .build();

            // 3. 将字段封装进 Schema (当前最推荐的方式)
            CollectionSchemaParam schema = CollectionSchemaParam.newBuilder()
                    .addFieldType(idField)
                    .addFieldType(vectorField)
                    .addFieldType(contentField)
                    .addFieldType(metadataField)
                    .build();

            // 4. 创建集合
            client.createCollection(CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withSchema(schema)
                    .build());

            // 5. 创建索引 (使用 FLAT 索引以获得最高兼容性)
            client.createIndex(CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName("embedding")
                    .withIndexType(IndexType.FLAT)
                    .withMetricType(MetricType.COSINE)
                    .build());

            // 6. 加载集合到内存 (搜索和插入前必须 load)
            client.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withSyncLoad(false) // 禁用同步加载，防止 SDK 内部 showCollections 检查时由于 percentage 数量不匹配抛出 IllegalResponseException
                    .build());
            
            log.info("Milvus 集合与索引创建并加载成功");

        } catch (Exception e) {
            log.error("手动初始化 Milvus 架构失败: {}", e.getMessage(), e);
        }
    }
}
