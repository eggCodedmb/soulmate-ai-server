package com.soulmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.soulmate.common.exception.BizException;
import com.soulmate.common.response.ResultCode;
import com.soulmate.domain.entity.*;
import com.soulmate.mapper.*;
import com.soulmate.service.CompanionService;
import com.soulmate.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanionServiceImpl extends ServiceImpl<CompanionMapper, Companion>
        implements CompanionService {

    private final CompanionPersonalityMapper personalityMapper;
    private final CompanionVoiceMapper voiceMapper;
    private final CompanionAvatarMapper avatarMapper;
    private final CompanionReminderMapper reminderMapper;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final MemoryMapper memoryMapper;
    private final MemoryTagMapper memoryTagMapper;
    private final NotificationMapper notificationMapper;
    private final ScheduleReminderMapper scheduleReminderMapper;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional
    public Companion createCompanion(Long userId, Companion companion, List<CompanionPersonality> personalities) {
        // 检查伴侣数量限制
        if (!subscriptionService.checkCompanionLimit(userId)) {
            throw new BizException(ResultCode.COMPANION_LIMIT_REACHED);
        }

        // 保存伴侣
        companion.setUserId(userId);
        companion.setStatus(1);
        companion.setCompanionOrder(0);
        companion.setCreateTime(LocalDateTime.now());
        companion.setUpdateTime(LocalDateTime.now());
        save(companion);

        // 保存性格标签
        if (personalities != null) {
            for (CompanionPersonality personality : personalities) {
                personality.setCompanionId(companion.getId());
                personality.setCreateTime(LocalDateTime.now());
                personalityMapper.insert(personality);
            }
        }

        // 根据性格特征设置主题色
        companion.setThemeColor(resolveThemeColor(personalities));
        updateById(companion);

        return companion;
    }

    @Override
    public List<Companion> getUserCompanions(Long userId) {
        List<Companion> companions = list(new LambdaQueryWrapper<Companion>()
                .eq(Companion::getUserId, userId)
                .eq(Companion::getStatus, 1)
                .orderByDesc(Companion::getCompanionOrder)
                .orderByDesc(Companion::getCreateTime));
        companions.forEach(this::fillPersonalityKeys);
        return companions;
    }

    @Override
    public Companion getCompanionDetail(Long userId, Long companionId) {
        Companion companion = getById(companionId);
        if (companion == null || !companion.getUserId().equals(userId)) {
            throw new BizException(ResultCode.COMPANION_NOT_FOUND);
        }
        fillPersonalityKeys(companion);
        return companion;
    }

    @Override
    @Transactional
    public void updateCompanion(Long userId, Long companionId, Companion companion, List<CompanionPersonality> personalities) {
        Companion existing = getCompanionDetail(userId, companionId);
        companion.setId(companionId);
        companion.setUserId(userId);
        companion.setUpdateTime(LocalDateTime.now());

        // 更新个性标签（仅当传入时不为null才更新）
        if (personalities != null) {
            // 逻辑删除旧标签
            personalityMapper.delete(new LambdaQueryWrapper<CompanionPersonality>()
                    .eq(CompanionPersonality::getCompanionId, companionId));
            // 插入新标签
            for (CompanionPersonality personality : personalities) {
                personality.setCompanionId(companionId);
                personality.setCreateTime(LocalDateTime.now());
                personalityMapper.insert(personality);
            }
            // 重新计算主题色
            companion.setThemeColor(resolveThemeColor(personalities));
        }

        updateById(companion);
    }

    @Override
    @Transactional
    public void deleteCompanion(Long userId, Long companionId) {
        Companion existing = getCompanionDetail(userId, companionId);
        
        // 1. 逻辑删除伴侣主表记录
        existing.setStatus(0); // 归档
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
        removeById(companionId); // MyBatis-Plus 会根据配置执行逻辑删除 (deleted=1)

        // 2. 级联删除相关配置
        // 性格标签
        personalityMapper.delete(new LambdaQueryWrapper<CompanionPersonality>()
                .eq(CompanionPersonality::getCompanionId, companionId));
        
        // 语音配置
        voiceMapper.delete(new LambdaQueryWrapper<CompanionVoice>()
                .eq(CompanionVoice::getCompanionId, companionId));
        
        // 头像配置
        avatarMapper.delete(new LambdaQueryWrapper<CompanionAvatar>()
                .eq(CompanionAvatar::getCompanionId, companionId));

        // 伴侣定时提醒
        reminderMapper.delete(new LambdaQueryWrapper<CompanionReminder>()
                .eq(CompanionReminder::getCompanionId, companionId));

        // 3. 级联删除对话与消息
        List<Conversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getCompanionId, companionId));
        
        for (Conversation conv : conversations) {
            // 删除消息
            messageMapper.delete(new LambdaQueryWrapper<Message>()
                    .eq(Message::getConversationId, conv.getId()));
            // 删除会话
            conversationMapper.deleteById(conv.getId());
        }

        // 4. 级联删除记忆
        List<Memory> memories = memoryMapper.selectList(
                new LambdaQueryWrapper<Memory>()
                        .eq(Memory::getCompanionId, companionId));
        
        for (Memory memory : memories) {
            // 删除记忆标签
            memoryTagMapper.delete(new LambdaQueryWrapper<MemoryTag>()
                    .eq(MemoryTag::getMemoryId, memory.getId()));
            // 删除记忆
            memoryMapper.deleteById(memory.getId());
        }

        // 5. 级联删除通知与日程
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getCompanionId, companionId));
        
        scheduleReminderMapper.delete(new LambdaQueryWrapper<ScheduleReminder>()
                .eq(ScheduleReminder::getCompanionId, companionId));

        log.info("伴侣及其关联数据已成功级联删除: companionId={}, userId={}", companionId, userId);
    }

    @Override
    public List<CompanionPersonality> getCompanionPersonalities(Long companionId) {
        return personalityMapper.selectList(
                new LambdaQueryWrapper<CompanionPersonality>()
                        .eq(CompanionPersonality::getCompanionId, companionId));
    }

    @Override
    public CompanionVoice getCompanionVoice(Long companionId) {
        return voiceMapper.selectOne(
                new LambdaQueryWrapper<CompanionVoice>()
                        .eq(CompanionVoice::getCompanionId, companionId));
    }

    @Override
    public void updateAvatar(Long userId, Long companionId, String avatarUrl) {
        Companion companion = getCompanionDetail(userId, companionId);
        companion.setAvatarUrl(avatarUrl);
        companion.setUpdateTime(LocalDateTime.now());
        updateById(companion);
    }

    /**
     * 填充伴侣的性格标签
     */
    private void fillPersonalityKeys(Companion companion) {
        List<CompanionPersonality> personalities = getCompanionPersonalities(companion.getId());
        companion.setPersonalityKeys(
                personalities.stream()
                        .map(p -> p.getPersonalityKey().getCode())
                        .toList());
    }

    /**
     * 根据性格标签解析主题色
     */
    private String resolveThemeColor(List<CompanionPersonality> personalities) {
        if (personalities == null || personalities.isEmpty()) {
            return "#FFE4EC"; // 默认温柔色
        }
        // 取第一个性格标签对应的颜色
        return switch (personalities.get(0).getPersonalityKey()) {
            case GENTLE -> "#FFE4EC";
            case LIVELY -> "#FFF3E0";
            case CALM -> "#E3F2FD";
            case HUMOROUS -> "#FFFDE7";
            case INTELLECTUAL -> "#F3E5F5";
            case COOL -> "#ECEFF1";
        };
    }
}
