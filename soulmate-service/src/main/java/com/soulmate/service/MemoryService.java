package com.soulmate.service;

import com.soulmate.domain.entity.Memory;
import com.soulmate.domain.enums.MemoryCategory;

import java.util.List;

/**
 * 记忆服务
 */
public interface MemoryService {

    /**
     * 获取用户的记忆列表
     */
    List<Memory> getUserMemories(Long userId, Long companionId, MemoryCategory category);

    /**
     * 编辑记忆
     */
    void updateMemory(Long userId, Long memoryId, Memory memory);

    /**
     * 删除记忆
     */
    void deleteMemory(Long userId, Long memoryId);

    /**
     * 从对话中提取记忆（异步调用）
     */
    void extractMemories(Long userId, Long companionId, Long conversationId);

    /**
     * RAG 检索相关记忆
     */
    List<Memory> retrieveRelevantMemories(Long userId, Long companionId, String query);
}
