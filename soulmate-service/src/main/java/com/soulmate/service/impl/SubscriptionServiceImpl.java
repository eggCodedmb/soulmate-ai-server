package com.soulmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soulmate.common.config.LimitProperties;
import com.soulmate.common.constant.RedisConstants;
import com.soulmate.domain.entity.Companion;
import com.soulmate.domain.entity.SubscriptionPlan;
import com.soulmate.domain.entity.UserSubscription;
import com.soulmate.domain.enums.SubscriptionStatus;
import com.soulmate.mapper.CompanionMapper;
import com.soulmate.mapper.SubscriptionPlanMapper;
import com.soulmate.mapper.UserSubscriptionMapper;
import com.soulmate.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionPlanMapper planMapper;
    private final UserSubscriptionMapper subscriptionMapper;
    private final CompanionMapper companionMapper;
    private final StringRedisTemplate redisTemplate;
    private final LimitProperties limitProperties;

    @Override
    public List<SubscriptionPlan> getAllPlans() {
        return planMapper.selectList(
                new LambdaQueryWrapper<SubscriptionPlan>()
                        .eq(SubscriptionPlan::getStatus, 1)
                        .orderByAsc(SubscriptionPlan::getDisplayOrder));
    }

    @Override
    public UserSubscription getCurrentSubscription(Long userId) {
        return subscriptionMapper.selectOne(
                new LambdaQueryWrapper<UserSubscription>()
                        .eq(UserSubscription::getUserId, userId)
                        .eq(UserSubscription::getStatus, SubscriptionStatus.ACTIVE)
                        .orderByDesc(UserSubscription::getEndTime)
                        .last("LIMIT 1"));
    }

    @Override
    public SubscriptionPlan getUserPlan(Long userId) {
        UserSubscription subscription = getCurrentSubscription(userId);
        if (subscription == null) {
            // 返回免费套餐
            return planMapper.selectOne(
                    new LambdaQueryWrapper<SubscriptionPlan>()
                            .eq(SubscriptionPlan::getPlanCode, "FREE"));
        }
        return planMapper.selectById(subscription.getPlanId());
    }

    @Override
    public boolean checkDailyMessageLimit(Long userId) {
        SubscriptionPlan plan = getUserPlan(userId);
        if (plan.getMaxDailyMessages() == -1) {
            return true; // 无限制
        }

        String key = RedisConstants.USER_DAILY_MSG + userId + ":" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String count = redisTemplate.opsForValue().get(key);
        int currentCount = count == null ? 0 : Integer.parseInt(count);
        return currentCount < plan.getMaxDailyMessages();
    }

    @Override
    public void incrementDailyMessageCount(Long userId) {
        String key = RedisConstants.USER_DAILY_MSG + userId + ":" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 设置过期时间为当天结束
            long secondsUntilMidnight = LocalDateTime.now().until(
                    LocalDate.now().plusDays(1).atStartOfDay(),
                    java.time.temporal.ChronoUnit.SECONDS);
            redisTemplate.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);
        }
    }

    @Override
    public boolean checkCompanionLimit(Long userId) {
        SubscriptionPlan plan = getUserPlan(userId);
        if (plan.getMaxCompanions() == -1) {
            return true; // 无限制
        }
        // 查询当前活跃伴侣数
        long currentCount = companionMapper.selectCount(
                new LambdaQueryWrapper<Companion>()
                        .eq(Companion::getUserId, userId)
                        .eq(Companion::getStatus, 1));
        return currentCount < plan.getMaxCompanions();
    }

    @Override
    public void activateSubscription(Long userId, Long planId) {
        UserSubscription newSub = new UserSubscription();
        newSub.setUserId(userId);
        newSub.setPlanId(planId);
        newSub.setStartTime(LocalDateTime.now());
        newSub.setEndTime(LocalDateTime.now().plusMonths(1));
        newSub.setAutoRenew(1);
        newSub.setStatus(SubscriptionStatus.ACTIVE);
        newSub.setCreateTime(LocalDateTime.now());
        newSub.setUpdateTime(LocalDateTime.now());
        subscriptionMapper.insert(newSub);
    }
}
