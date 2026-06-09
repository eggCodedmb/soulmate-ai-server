# Spring AI 架构修复总结

## 修复的问题

### 1. 高优先级 - 启动失败风险修复

**问题**: 排除了 `TransformersEmbeddingModelAutoConfiguration` 但没有替代的 `EmbeddingModel` Bean，导致 Milvus 可达时应用启动失败。

**修复**:
- 修改 `SoulMateApplication.java`，移除对 `TransformersEmbeddingModelAutoConfiguration` 的排除
- 让 Spring AI 自动创建 `EmbeddingModel` Bean

**文件**: `soulmate-app/src/main/java/com/soulmate/SoulMateApplication.java`

### 2. 高优先级 - 敏感信息管理统一

**问题**: API Key、数据库密码等敏感信息硬编码在配置文件中。

**修复**: 将敏感信息统一改为环境变量配置：
- 数据库连接: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- Redis: `REDIS_HOST`, `REDIS_PORT`
- Spring AI: `MIMO_API_KEY`, `MIMO_BASE_URL`
- Milvus: `MILVUS_HOST`, `MILVUS_PORT`
- 邮件: `MAIL_USERNAME`, `MAIL_PASSWORD` (已有)
- 天气: `QWEATHER_API_KEY`, `QWEATHER_API_HOST` (已有)

**文件**: `soulmate-app/src/main/resources/application.yml`

### 3. 中优先级 - MCP 配置清理

**问题**: 引入了 `spring-ai-starter-mcp-server` 但实际只使用了本地 Tool Calling。

**修复**:
- 移除 `spring-ai-starter-mcp-server` 依赖
- 移除 `spring.ai.mcp.server.*` 配置
- 重命名 `WeatherMcpService` 为 `WeatherToolService`

**文件**:
- `soulmate-ai/pom.xml`
- `soulmate-app/src/main/resources/application.yml`
- `soulmate-ai/src/main/java/com/soulmate/ai/mcp/WeatherToolService.java` (新文件)
- `soulmate-ai/src/main/java/com/soulmate/ai/mcp/WeatherMcpService.java` (已删除)

### 4. 中优先级 - 配置重复清理

**问题**: AI 模型参数在 `spring.ai.openai.*` 和 `soulmate.ai.*` 中重复定义。

**修复**:
- 精简 `AiProperties` 类，移除重复字段
- 移除 `ChatServiceImpl` 中未使用的 `AiProperties` 注入
- 清理 `application.yml` 中 `soulmate.ai.*` 下的重复配置

**文件**:
- `soulmate-common/src/main/java/com/soulmate/common/config/AiProperties.java`
- `soulmate-service/src/main/java/com/soulmate/service/impl/ChatServiceImpl.java`
- `soulmate-app/src/main/resources/application.yml`

### 5. 中优先级 - 向量存储配置优化

**问题**: `VectorStoreConfig` 中硬编码了配置参数，与 yml 配置重复。

**修复**: 将硬编码参数改为从配置文件读取：
- `collectionName`
- `indexType`
- `metricType`
- `initializeSchema`

**文件**: `soulmate-service/src/main/java/com/soulmate/service/config/VectorStoreConfig.java`

### 6. 低优先级 - 依赖冗余清理

**问题**: `spring-ai-starter-model-openai` 在两个模块中重复声明。

**修复**: 移除 `soulmate-service/pom.xml` 中冗余的 `spring-ai-starter-model-openai` 依赖。

**文件**: `soulmate-service/pom.xml`

## 未修复的问题

### 1. 模块职责错位

**问题**: `soulmate-ai` 模块名存实虚，核心 AI 逻辑在 `soulmate-service` 中。

**建议**: 
- 方案一: 将 `ChatServiceImpl`、`PromptBuilder`、`MemoryServiceImpl`、`VectorStoreConfig` 移入 `soulmate-ai` 模块
- 方案二: 取消 `soulmate-ai` 模块，将 `WeatherToolService` 移入 `soulmate-service`

**原因**: 这需要较大的重构，影响范围广，建议在后续版本中处理。

### 2. 仓库声明散落

**问题**: `soulmate-ai/pom.xml`、`soulmate-app/pom.xml` 都声明了 Spring 仓库。

**建议**: 统一到父 POM 中。

**原因**: 这是小问题，不影响功能，可以在后续清理。

## 验证结果

编译成功，所有模块正常构建：

```
[INFO] Reactor Summary for SoulMate AI 1.0.0-SNAPSHOT:
[INFO] 
[INFO] SoulMate AI ........................................ SUCCESS [  0.116 s]
[INFO] soulmate-common .................................... SUCCESS [  2.051 s]
[INFO] soulmate-domain .................................... SUCCESS [  1.519 s]
[INFO] soulmate-mapper .................................... SUCCESS [  0.799 s]
[INFO] soulmate-ai ........................................ SUCCESS [  0.788 s]
[INFO] soulmate-service ................................... SUCCESS [  1.922 s]
[INFO] soulmate-web ....................................... SUCCESS [  1.430 s]
[INFO] soulmate-app ....................................... SUCCESS [  1.035 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

## 环境变量配置示例

创建 `.env` 文件或设置系统环境变量：

```bash
# 数据库
DB_URL=jdbc:postgresql://localhost:5432/soulmate
DB_USERNAME=postgres
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Spring AI (mimo-v2.5-pro)
MIMO_API_KEY=your_api_key
MIMO_BASE_URL=https://token-plan-sgp.xiaomimimo.com/v1

# Milvus
MILVUS_HOST=192.168.2.114
MILVUS_PORT=19530

# 邮件
MAIL_USERNAME=your_email@qq.com
MAIL_PASSWORD=your_email_password

# 天气
QWEATHER_API_KEY=your_qweather_key
QWEATHER_API_HOST=https://your_qweather_host
```

## 后续建议

1. **监控 EmbeddingModel 初始化**: 由于启用了 `TransformersEmbeddingModelAutoConfiguration`，应用启动时会下载 ONNX 模型，首次启动可能较慢。建议监控启动日志。

2. **Milvus 连接健康检查**: 当前 `VectorStoreConfig` 在启动时尝试连接 Milvus，但 gRPC 客户端通常是懒连接的。建议添加运行时健康检查。

3. **配置加密**: 对于生产环境，建议使用 Spring Cloud Config 或 HashiCorp Vault 管理敏感配置，而不是环境变量。

4. **模块重构**: 考虑将 AI 相关代码集中到 `soulmate-ai` 模块，提高代码组织性。
