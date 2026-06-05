package com.soulmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soulmate.common.exception.BizException;
import com.soulmate.common.response.ResultCode;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记忆服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {

    private final MemoryMapper memoryMapper;
    private final MemoryTagMapper memoryTagMapper;
    private final MessageMapper messageMapper;
    private final ConversationMapper conversationMapper;
    private final CompanionMapper companionMapper;
    private final CompanionPersonalityMapper personalityMapper;
    private final ChatClient.Builder chatClientBuilder;

    @Override
    public List<Memory> getUserMemories(Long userId, Long companionId, MemoryCategory category) {
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
    }

    @Override
    public void deleteMemory(Long userId, Long memoryId) {
        Memory existing = memoryMapper.selectById(memoryId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BizException("记忆不存在");
        }
        memoryMapper.deleteById(memoryId);
    }

    @Override
    @Async
    @Transactional
    public void extractMemories(Long userId, Long companionId, Long conversationId) {
        try {
            // 获取最近的对话消息
            List<Message> recentMessages = messageMapper.selectList(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getConversationId, conversationId)
                            .eq(Message::getDeleted, 0)
                            .orderByDesc(Message::getCreateTime)
                            .last("LIMIT 20"));

            if (recentMessages.isEmpty()) {
                return;
            }

            // 构建对话文本
            StringBuilder conversationText = new StringBuilder();
            for (Message msg : recentMessages) {
                String role = msg.getSenderType() == SenderType.USER ? "用户" : "AI";
                conversationText.append(role).append(": ").append(msg.getContent()).append("\n");
            }

            // 使用 LLM 提取记忆
            String extractPrompt = """
                    请从以下对话中提取值得记住的信息，以JSON数组格式返回。
                    每条记忆包含: category (personal_info/shared_experience/preference/habit), title, content, importance (1-10)
                    如果没有值得记住的信息，返回空数组 []

                    对话内容:
                    %s
                    """.formatted(conversationText);

            String response = chatClientBuilder.build()
                    .prompt()
                    .user(extractPrompt)
                    .call()
                    .content();

            // 简单解析（生产环境应使用JSON解析）
            if (response != null && response.contains("\"title\"")) {
                // 保存记忆（简化处理，生产环境需要完整JSON解析）
                Memory memory = new Memory();
                memory.setUserId(userId);
                memory.setCompanionId(companionId);
                memory.setCategory(MemoryCategory.SHARED_EXPERIENCE);
                memory.setTitle("对话记忆");
                memory.setContent(truncate(response, 500));
                memory.setImportance(5);
                memory.setUserVisible(1);
                memory.setUserEdited(0);
                memory.setAccessCount(0);
                memory.setCreateTime(LocalDateTime.now());
                memory.setUpdateTime(LocalDateTime.now());
                memoryMapper.insert(memory);

                log.info("记忆提取成功: userId={}, companionId={}, memoryId={}", userId, companionId, memory.getId());
            }

        } catch (Exception e) {
            log.warn("记忆提取失败: userId={}, conversationId={}", userId, conversationId, e);
        }
    }

    @Override
    public List<Memory> retrieveRelevantMemories(Long userId, Long companionId, String query) {
        // 简单实现：基于关键词匹配（生产环境应使用 Milvus 向量检索）
        // 暂时返回最近的重要记忆
        return memoryMapper.selectList(
                new LambdaQueryWrapper<Memory>()
                        .eq(Memory::getUserId, userId)
                        .eq(Memory::getCompanionId, companionId)
                        .eq(Memory::getUserVisible, 1)
                        .eq(Memory::getDeleted, 0)
                        .ge(Memory::getImportance, 5)
                        .orderByDesc(Memory::getImportance)
                        .orderByDesc(Memory::getLastAccessTime)
                        .last("LIMIT 5"));
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
