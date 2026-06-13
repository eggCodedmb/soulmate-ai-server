package com.soulmate.service;

import com.soulmate.domain.dto.SubscriptionStatusDTO;
import com.soulmate.domain.entity.SubscriptionPlan;
import com.soulmate.domain.entity.UserSubscription;

import java.util.List;

/**
 * 订阅服务
 */
public interface SubscriptionService {

    /**
     * 获取用户当前额度状态
     */
    SubscriptionStatusDTO getSubscriptionStatus(Long userId);

    /**
     * 获取所有套餐
     */
    List<SubscriptionPlan> getAllPlans();

    /**
     * 获取用户当前订阅
     */
    UserSubscription getCurrentSubscription(Long userId);

    /**
     * 获取用户的套餐信息
     */
    SubscriptionPlan getUserPlan(Long userId);

    /**
     * 检查每日消息限制
     */
    boolean checkDailyMessageLimit(Long userId);

    /**
     * 增加每日消息计数
     */
    void incrementDailyMessageCount(Long userId);

    /**
     * 检查伴侣数量限制
     */
    boolean checkCompanionLimit(Long userId);

    /**
     * 激活订阅
     */
    void activateSubscription(Long userId, Long planId);
}
