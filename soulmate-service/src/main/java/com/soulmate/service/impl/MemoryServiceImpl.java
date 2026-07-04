package com.soulmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soulmate.common.exception.BizException;
import com.soulmate.common.util.VectorIdUtil;
import com.soulmate.domain.dto.MemoryDTO;
import com.soulmate.domain.entity.Memory;
import com.soulmate.domain.entity.Message;
import com.soulmate.domain.enums.MemoryCategory;
import com.soulmate.domain.enums.SenderType;
import com.soulmate.mapper.CompanionMapper;
import com.soulmate.mapper.CompanionPersonalityMapper;
import com.soulmate.mapper.ConversationMapper;
import com.soulmate.mapper.MemoryMapper;
import com.soulmate.mapper.MemoryTagMapper;
import com.soulmate.mapper.MessageMapper;
import com.soulmate.domain.dto.MemoryStatsDTO;
import com.soulmate.service.MemoryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

/**
 * 记忆服务实现
 */
@Slf4j
@Service
public class MemoryServiceImpl implements MemoryService {

    private final MemoryMapper memoryMapper;
    private final MessageMapper messageMapper;
    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;

    @Value("${spring.ai.vectorstore.pgvector.table-name:memory_vectors}")
    private String collectionName;

    public MemoryServiceImpl(MemoryMapper memoryMapper, MemoryTagMapper memoryTagMapper,
                             MessageMapper messageMapper, ConversationMapper conversationMapper,
                             CompanionMapper companionMapper, CompanionPersonalityMapper personalityMapper,
                             ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper,
                             @Nullable VectorStore vectorStore) {
        this.memoryMapper = memoryMapper;
        this.messageMapper = messageMapper;
        this.chatClientBuilder = chatClientBuilder;
        this.objectMapper = objectMapper;
        this.vectorStore = vectorStore;
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    @Override
    public List<MemoryDTO> listMemories(Long userId, Long companionId, MemoryCategory category) {
        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getUserVisible, 1)
                .eq(Memory::getDeleted, 0);

        if (companionId != null) {
            wrapper.eq(Memory::getCompanionId, companionId);
        }
        if (category != null) {
            wrapper.eq(Memory::getCategory, category);
        }

        wrapper.orderByDesc(Memory::getCreateTime);

        List<Memory> list = memoryMapper.selectList(wrapper);
        return list.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public MemoryStatsDTO getMemoryStats(Long userId, Long companionId) {
        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getUserVisible, 1)
                .eq(Memory::getDeleted, 0);

        if (companionId != null) {
            wrapper.eq(Memory::getCompanionId, companionId);
        }

        Long total = memoryMapper.selectCount(wrapper);
        MemoryStatsDTO stats = new MemoryStatsDTO();
        stats.setTotalMemories(total.intValue());
        stats.setCategoryCount(MemoryCategory.values().length);

        if (total == 0) {
            stats.setAverageImportance(0.0);
            return stats;
        }

        // 直接用 SQL 聚合，避免全量加载
        List<Memory> list = memoryMapper.selectList(wrapper.select(Memory::getImportance));
        double avg = list.stream()
                .mapToInt(m -> m.getImportance() != null ? m.getImportance() : 0)
                .average()
                .orElse(0.0);
        stats.setAverageImportance(Math.round(avg * 10.0) / 10.0);

        return stats;
    }

    private MemoryDTO convertToDTO(Memory memory) {
        MemoryDTO dto = new MemoryDTO();
        dto.setId(memory.getId());
        dto.setUserId(memory.getUserId());
        dto.setCompanionId(memory.getCompanionId());
        dto.setCategory(memory.getCategory());
        dto.setCategoryDesc(memory.getCategory().getDesc());
        dto.setTitle(memory.getTitle());
        dto.setContent(memory.getContent());
        dto.setThought(memory.getThought());
        dto.setEmotion(memory.getEmotion());
        dto.setImportance(memory.getImportance());
        dto.setUserEdited(memory.getUserEdited());
        dto.setCreateTime(memory.getCreateTime());
        dto.setTimeDesc(memory.getCreateTime().format(DATE_FORMATTER));
        return dto;
    }

    @Override
    @Transactional
    public void saveMemory(Long userId, Memory memory) {
        memory.setUserId(userId);
        memory.setUserEdited(1);
        memory.setUserVisible(1);
        memory.setAccessCount(0);
        memory.setCreateTime(LocalDateTime.now());
        memory.setUpdateTime(LocalDateTime.now());
        memory.setLastAccessTime(LocalDateTime.now());
        memoryMapper.insert(memory);
        syncToVectorStore(memory, userId);
    }

    public List<Memory> getUserMemories(Long userId, Long companionId, MemoryCategory category) {
        // Keep this for internal use or RAG if needed, but listMemories is for client
        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getUserVisible, 1)
                .eq(Memory::getDeleted, 0);

        if (companionId != null) {
            wrapper.eq(Memory::getCompanionId, companionId);
        }
        if (category != null) {
            wrapper.eq(Memory::getCategory, category);
        }

        wrapper.orderByDesc(Memory::getImportance)
               .orderByDesc(Memory::getCreateTime);

        return memoryMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void updateMemory(Long userId, Long memoryId, Memory memory) {
        Memory existing = memoryMapper.selectById(memoryId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BizException("记忆不存在");
        }

        existing.setTitle(memory.getTitle());
        existing.setContent(memory.getContent());
        existing.setCategory(memory.getCategory());
        existing.setUserEdited(1);
        existing.setUpdateTime(LocalDateTime.now());
        memoryMapper.updateById(existing);
        syncToVectorStore(existing, userId);
    }

    @Override
    @Transactional
    public void deleteMemory(Long userId, Long memoryId) {
        Memory existing = memoryMapper.selectById(memoryId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BizException("记忆不存在");
        }
        memoryMapper.deleteById(memoryId);
        deleteFromVectorStore(memoryId);
    }

    @Override
    @Async
    @Transactional
    public void extractMemories(Long userId, Long companionId, Long conversationId) {
        try {
            // 获取最近的对话消息 (增加到50条以便提取更完整的信息)
            List<Message> recentMessages = messageMapper.selectList(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getConversationId, conversationId)
                            .eq(Message::getDeleted, 0)
                            .orderByDesc(Message::getCreateTime)
                            .last("LIMIT 50"));

            if (recentMessages.size() < 2) { // 至少一问一答才值得提取
                return;
            }

            // 构建对话文本 (正序排列)
            StringBuilder conversationText = new StringBuilder();
            List<Message> reversedMessages = recentMessages.stream()
                    .sorted((a, b) -> a.getCreateTime().compareTo(b.getCreateTime()))
                    .collect(Collectors.toList());

            for (Message msg : reversedMessages) {
                String role = msg.getSenderType() == SenderType.USER ? "用户" : "AI";
                conversationText.append(role).append(": ").append(msg.getContent()).append("\n");
            }

            // 使用 LLM 提取记忆
            String extractPrompt = """
                    你是一个记忆提取专家。请从以下对话中提取用户分享的、值得伴侣长期记住的关键信息。
                    重点关注：用户的兴趣爱好、日常习惯、个人经历、对事物的看法、对伴侣的要求，以及私密爱好等。
                    
                    输出要求：
                    1. 必须返回 JSON 数组格式
                    2. 每条记忆包含以下字段: 
                       - category: 枚举值。必须使用以下值之一：
                         * personal_info: 个人基本信息（如姓名、职业、生日、家人等）。
                         * shared_experience: 共同经历（用户与AI共同发生过的事情或讨论过的重要事件）。
                         * preference: 偏好习惯。仅限日常公共偏好，如食物口味（喜欢吃辣/不喜欢香菜）、兴趣爱好（看电影/打篮球）、颜色/作息等日常公共层面的偏好。**绝对不要包含任何性或亲密隐私方面的内容。**
                         * habit: 日常习惯行为。
                         * private_preference: 私密爱好。专门记录主人和伴侣在性与亲密隐私方面的偏好，包括敏感部位、喜欢的部位、性喜好、性幻想等私密层面的爱好。**与日常公共偏好严格区分。**
                       - title: 简短的标题 (10字以内)
                       - content: 具体的记忆内容 (50字以内，以第三人称描述用户，例如“用户喜欢吃辣”)
                       - thought: AI的内心独白/感悟 (20字以内，例如“他竟然不喜欢吃香菜，记下来下次避开”)
                       - emotion: 情绪标签 (枚举值: happy, sad, warm, funny, neutral)
                       - importance: 重要程度 (1-10)
                    3. 如果没有发现值得记住的新信息，请返回空数组 []
                    4. 不要返回任何解释性文字，只返回 JSON
                    
                    对话内容:
                    %s
                    """.formatted(conversationText);

            String response = chatClientBuilder.build()
                    .prompt()
                    .user(extractPrompt)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                return;
            }

            // 清理 JSON (有些 LLM 会返回带有前导解释文字或 markdown 块的文本)
            String json = response.trim();
            int codeBlockStart = json.indexOf("```json");
            if (codeBlockStart != -1) {
                int codeBlockEnd = json.indexOf("```", codeBlockStart + 7);
                if (codeBlockEnd != -1) {
                    json = json.substring(codeBlockStart + 7, codeBlockEnd).trim();
                }
            } else {
                int codeBlockStartGeneric = json.indexOf("```");
                if (codeBlockStartGeneric != -1) {
                    int codeBlockEndGeneric = json.indexOf("```", codeBlockStartGeneric + 3);
                    if (codeBlockEndGeneric != -1) {
                        json = json.substring(codeBlockStartGeneric + 3, codeBlockEndGeneric).trim();
                    }
                }
            }
            
            // 确保只提取 JSON 数组 [ ... ] 部分
            int arrayStart = json.indexOf("[");
            int arrayEnd = json.lastIndexOf("]");
            if (arrayStart == -1 || arrayEnd == -1 || arrayEnd <= arrayStart) {
                log.info("未从AI响应中找到JSON数组结构，跳过记忆提取。userId={}, conversationId={}, response={}", 
                        userId, conversationId, response.trim());
                return;
            }
            json = json.substring(arrayStart, arrayEnd + 1).trim();

            List<Map<String, Object>> extractedList;
            try {
                extractedList = objectMapper.readValue(json, new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("解析AI提取的记忆JSON失败: userId={}, conversationId={}, json={}, error={}", 
                        userId, conversationId, json, e.getMessage());
                return;
            }
            
            for (Map<String, Object> item : extractedList) {
                try {
                    Memory memory = new Memory();
                    memory.setUserId(userId);
                    memory.setCompanionId(companionId);
                    
                    // 类别转换
                    String categoryStr = (String) item.get("category");
                    MemoryCategory category = MemoryCategory.PERSONAL_INFO;
                    for (MemoryCategory mc : MemoryCategory.values()) {
                        if (mc.getCode().equalsIgnoreCase(categoryStr)) {
                            category = mc;
                            break;
                        }
                    }
                    
                    memory.setCategory(category);
                    memory.setTitle((String) item.get("title"));
                    memory.setContent((String) item.get("content"));
                    memory.setThought((String) item.get("thought"));
                    memory.setEmotion((String) item.get("emotion"));
                    memory.setImportance(Integer.parseInt(item.get("importance").toString()));
                    memory.setUserVisible(1);
                    memory.setUserEdited(0);
                    memory.setAccessCount(0);
                    memory.setCreateTime(LocalDateTime.now());
                    memory.setUpdateTime(LocalDateTime.now());
                    memory.setLastAccessTime(LocalDateTime.now());
                    
                    // 查重：如果最近已经有类似内容的消息，则跳过
                    Long count = memoryMapper.selectCount(new LambdaQueryWrapper<Memory>()
                            .eq(Memory::getUserId, userId)
                            .eq(Memory::getCompanionId, companionId)
                            .eq(Memory::getContent, memory.getContent())
                            .eq(Memory::getDeleted, 0));
                    
                    if (count == 0) {
                        memoryMapper.insert(memory);
                        syncToVectorStore(memory, userId);
                        log.info("提取新记忆并同步向量库成功: title={}", memory.getTitle());
                    }
                } catch (Exception e) {
                    log.warn("解析单条记忆失败: {}", item, e);
                }
            }

        } catch (Exception e) {
            log.warn("记忆提取失败: userId={}, conversationId={}", userId, conversationId, e);
        }
    }

    @Override
    public List<Memory> retrieveRelevantMemories(Long userId, Long companionId, String query) {
        // 1. 语义搜索 (VectorStore)
        List<Long> memoryIdsFromVector = Collections.emptyList();
        if (vectorStore != null) {
            try {
                FilterExpressionBuilder b = new FilterExpressionBuilder();
                Filter.Expression filterExpression = b.and(
                        b.eq("userId", userId),
                        b.eq("companionId", companionId)
                ).build();

                SearchRequest searchRequest = SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .similarityThreshold(0.6)
                        .filterExpression(filterExpression)
                        .build();

                List<Document> results = vectorStore.similaritySearch(searchRequest);
                memoryIdsFromVector = results.stream()
                        .map(doc -> VectorIdUtil.fromVectorId(doc.getId()))
                        .collect(Collectors.toList());

                if (!memoryIdsFromVector.isEmpty()) {
                    log.debug("语义检索命中记忆: count={}, ids={}", memoryIdsFromVector.size(), memoryIdsFromVector);
                }
            } catch (Exception e) {
                log.warn("向量检索失败，回退到关键词搜索: userId={}, query={}", userId, query, e);
            }
        }

        // 2. 加载完整记忆数据
        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getCompanionId, companionId)
                .eq(Memory::getUserVisible, 1)
                .eq(Memory::getDeleted, 0);

        if (!memoryIdsFromVector.isEmpty()) {
            wrapper.in(Memory::getId, memoryIdsFromVector);
        } else {
            wrapper.and(w -> w.like(Memory::getContent, query)
                            .or().like(Memory::getTitle, query)
                            .or().ge(Memory::getImportance, 8))
                    .orderByDesc(Memory::getImportance)
                    .last("LIMIT 5");
        }

        List<Memory> memories = memoryMapper.selectList(wrapper);

        // 3. 更新访问计数
        if (!memories.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (Memory m : memories) {
                m.setAccessCount(m.getAccessCount() + 1);
                m.setLastAccessTime(now);
                memoryMapper.updateById(m);
            }
        }

        return memories;
    }

    @Override
    public void rebuildMemoryVectors() {
        if (vectorStore == null) {
            throw new BizException(500, "向量数据库组件未注入或不可用");
        }

        try {
            log.info("开始重建向量库");

            // 3. 从 MySQL 加载所有未删除的记忆数据
            List<Memory> allMemories = memoryMapper.selectList(new LambdaQueryWrapper<Memory>()
                    .eq(Memory::getDeleted, 0));
            log.info("查询到待同步的记忆记录数量: {}", allMemories.size());

            if (allMemories.isEmpty()) {
                log.info("没有需要同步的历史记忆");
                return;
            }

            // 4. 分批同步到向量库
            int batchSize = 100;
            for (int i = 0; i < allMemories.size(); i += batchSize) {
                List<Memory> batch = allMemories.subList(i, Math.min(i + batchSize, allMemories.size()));
                List<Document> documents = batch.stream()
                        .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                        .map(m -> new Document(
                                VectorIdUtil.toVectorId(m.getId()),
                                m.getContent(),
                                Map.of(
                                        "userId", m.getUserId(),
                                        "companionId", m.getCompanionId(),
                                        "category", m.getCategory() != null ? m.getCategory().getCode() : ""
                                )
                        ))
                        .collect(Collectors.toList());

                if (!documents.isEmpty()) {
                    vectorStore.add(documents);
                    log.info("成功同步 {} 条记忆数据到向量数据库", documents.size());
                }
            }
            log.info("向量数据库记忆重建及重新同步完成！");
        } catch (Exception e) {
            log.error("重建向量数据库失败", e);
            throw new BizException(500, "重建向量数据库失败: " + e.getMessage());
        }
    }

    private void syncToVectorStore(Memory memory, Long userId) {
        if (vectorStore == null) {
            return;
        }
        try {
            Document doc = new Document(VectorIdUtil.toVectorId(memory.getId()), memory.getContent(), Map.of(
                    "userId", userId,
                    "companionId", memory.getCompanionId(),
                    "category", memory.getCategory() != null ? memory.getCategory().getCode() : ""
            ));
            vectorStore.add(List.of(doc));
        } catch (Exception e) {
            log.warn("同步向量库失败: memoryId={}, title={}", memory.getId(), memory.getTitle(), e);
        }
    }

    private void deleteFromVectorStore(Long memoryId) {
        if (vectorStore == null) {
            return;
        }
        try {
            vectorStore.delete(List.of(VectorIdUtil.toVectorId(memoryId)));
        } catch (Exception e) {
            log.warn("从向量库删除记忆失败: memoryId={}", memoryId, e);
        }
    }
}
