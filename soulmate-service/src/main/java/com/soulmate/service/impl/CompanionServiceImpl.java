package com.soulmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.soulmate.common.exception.BizException;
import com.soulmate.common.response.ResultCode;
import com.soulmate.domain.entity.Companion;
import com.soulmate.domain.entity.CompanionPersonality;
import com.soulmate.domain.entity.CompanionVoice;
import com.soulmate.mapper.CompanionMapper;
import com.soulmate.mapper.CompanionPersonalityMapper;
import com.soulmate.mapper.CompanionVoiceMapper;
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
    public void deleteCompanion(Long userId, Long companionId) {
        Companion existing = getCompanionDetail(userId, companionId);
        existing.setStatus(0); // 归档
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
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
