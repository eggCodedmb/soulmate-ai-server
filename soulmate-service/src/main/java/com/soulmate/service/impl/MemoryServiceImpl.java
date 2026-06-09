package com.soulmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soulmate.common.exception.BizException;
import com.soulmate.common.response.ResultCode;
import com.soulmate.domain.dto.MemoryDTO;
import com.soulmate.domain.entity.Companion;
import com.soulmate.domain.entity.CompanionPersonality;
import com.soulmate.domain.entity.Conversation;
import com.soulmate.domain.entity.Memory;
import com.soulmate.domain.entity.MemoryTag;
import com.soulmate.domain.entity.Message;
import com.soulmate.domain.enums.ContentType;
import com.soulmate.domain.enums.MemoryCategory;
import com.soulmate.domain.enums.SenderType;
import com.soulmate.mapper.CompanionMapper;
import com.soulmate.mapper.CompanionPersonalityMapper;
import com.soulmate.mapper.ConversationMapper;
import com.soulmate.mapper.MemoryMapper;
import com.soulmate.mapper.MemoryTagMapper;
import com.soulmate.mapper.MessageMapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记忆服务实现
 */
@Slf4j
@Service
public class MemoryServiceImpl implements MemoryService {

    private final MemoryMapper memoryMapper;
    private final MemoryTagMapper memoryTagMapper;
    private final MessageMapper messageMapper;
    private final ConversationMapper conversationMapper;
    private final CompanionMapper companionMapper;
    private final CompanionPersonalityMapper personalityMapper;
    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;

    public MemoryServiceImpl(MemoryMapper memoryMapper, MemoryTagMapper memoryTagMapper,
                             MessageMapper messageMapper, ConversationMapper conversationMapper,
                             CompanionMapper companionMapper, CompanionPersonalityMapper personalityMapper,
                             ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper,
                             @Nullable VectorStore vectorStore) {
        this.memoryMapper = memoryMapper;
        this.memoryTagMapper = memoryTagMapper;
        this.messageMapper = messageMapper;
        this.conversationMapper = conversationMapper;
        this.companionMapper = companionMapper;
        this.personalityMapper = personalityMapper;
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

        // 同步到向量库
        if (vectorStore != null) {
            try {
                Document doc = new Document(memory.getId().toString(), memory.getContent(), Map.of(
                        "userId", userId,
                        "companionId", memory.getCompanionId(),
                        "category", memory.getCategory().getCode()
                ));
                vectorStore.add(List.of(doc));
            } catch (Exception e) {
                log.warn("手动保存记忆同步向量库失败: {}", memory.getTitle(), e);
            }
        }
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

        // 同步更新向量库
        if (vectorStore != null) {
            try {
                Document doc = new Document(existing.getId().toString(), existing.getContent(), Map.of(
                        "userId", userId,
                        "companionId", existing.getCompanionId(),
                        "category", existing.getCategory().getCode()
                ));
                vectorStore.add(List.of(doc));
            } catch (Exception e) {
                log.warn("同步更新向量库失败: memoryId={}", memoryId, e);
            }
        }
    }

    @Override
    @Transactional
    public void deleteMemory(Long userId, Long memoryId) {
        Memory existing = memoryMapper.selectById(memoryId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BizException("记忆不存在");
        }
        memoryMapper.deleteById(memoryId);

        // 同步从向量库删除
        if (vectorStore != null) {
            try {
                vectorStore.delete(List.of(memoryId.toString()));
            } catch (Exception e) {
                log.warn("从向量库删除记忆失败: memoryId={}", memoryId, e);
            }
        }
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
                    重点关注：用户的兴趣爱好、生活习惯、重要的个人经历、对事物的看法、对伴侣的要求等。
                    
                    输出要求：
                    1. 必须返回 JSON 数组格式
                    2. 每条记忆包含以下字段: 
                       - category: 枚举值 (personal_info, shared_experience, preference, habit)
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

            // 清理 JSON (有些 LLM 会返回 markdown 块)
            String json = response.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7, json.lastIndexOf("```")).trim();
            } else if (json.startsWith("```")) {
                json = json.substring(3, json.lastIndexOf("```")).trim();
            }

            List<Map<String, Object>> extractedList = objectMapper.readValue(json, new TypeReference<>() {});
            
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
                        
                        // 同步到向量库
                        if (vectorStore != null) {
                            try {
                                Document doc = new Document(memory.getId().toString(), memory.getContent(), Map.of(
                                        "userId", userId,
                                        "companionId", companionId,
                                        "category", category.getCode()
                                ));
                                vectorStore.add(List.of(doc));
                                log.info("提取新记忆并同步向量库成功: title={}", memory.getTitle());
                            } catch (Exception ve) {
                                log.warn("记忆同步向量库失败: title={}", memory.getTitle(), ve);
                            }
                        }
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
                // 构建过滤表达式: userId == userId AND companionId == companionId
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
                        .map(doc -> Long.parseLong(doc.getId()))
                        .collect(Collectors.toList());

                if (!memoryIdsFromVector.isEmpty()) {
                    log.debug("语义检索命中记忆: count={}, ids={}", memoryIdsFromVector.size(), memoryIdsFromVector);
                }
            } catch (Exception e) {
                log.warn("向量检索失败，回退到关键词搜索: userId={}, query={}", userId, query, e);
            }
        }

        // 2. 加载完整记忆数据 (如果语义搜索没中，回退到关键词匹配和高重要性记忆)
        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getCompanionId, companionId)
                .eq(Memory::getUserVisible, 1)
                .eq(Memory::getDeleted, 0);

        if (!memoryIdsFromVector.isEmpty()) {
            wrapper.in(Memory::getId, memoryIdsFromVector);
        } else {
            // 回退逻辑：关键词匹配或极高重要性
            wrapper.and(w -> w.like(Memory::getContent, query)
                            .or().like(Memory::getTitle, query)
                            .or().ge(Memory::getImportance, 8))
                    .orderByDesc(Memory::getImportance)
                    .last("LIMIT 5");
        }

        List<Memory> memories = memoryMapper.selectList(wrapper);
        
        // 3. 更新访问信息
        if (!memories.isEmpty()) {
            memories.forEach(m -> {
                m.setAccessCount(m.getAccessCount() + 1);
                m.setLastAccessTime(LocalDateTime.now());
                memoryMapper.updateById(m);
            });
        }
        
        return memories;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
