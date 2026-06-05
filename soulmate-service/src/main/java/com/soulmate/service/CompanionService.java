package com.soulmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.soulmate.domain.entity.Companion;
import com.soulmate.domain.entity.CompanionPersonality;
import com.soulmate.domain.entity.CompanionVoice;

import java.util.List;

/**
 * AI伴侣服务
 */
public interface CompanionService extends IService<Companion> {

    /**
     * 创建伴侣
     */
    Companion createCompanion(Long userId, Companion companion, List<CompanionPersonality> personalities);

    /**
     * 获取用户的伴侣列表
     */
    List<Companion> getUserCompanions(Long userId);

    /**
     * 获取伴侣详情
     */
    Companion getCompanionDetail(Long userId, Long companionId);

    /**
     * 编辑伴侣
     */
    void updateCompanion(Long userId, Long companionId, Companion companion);

    /**
     * 删除伴侣
     */
    void deleteCompanion(Long userId, Long companionId);

    /**
     * 获取伴侣的性格标签
     */
    List<CompanionPersonality> getCompanionPersonalities(Long companionId);

    /**
     * 获取伴侣的声音配置
     */
    CompanionVoice getCompanionVoice(Long companionId);
}
