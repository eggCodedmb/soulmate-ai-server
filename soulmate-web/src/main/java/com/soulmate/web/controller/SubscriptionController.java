package com.soulmate.web.controller;

import com.soulmate.common.response.R;
import com.soulmate.domain.dto.SubscriptionStatusDTO;
import com.soulmate.domain.entity.SubscriptionPlan;
import com.soulmate.domain.entity.UserSubscription;
import com.soulmate.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订阅控制器
 */
@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * 获取所有套餐
     */
    @GetMapping("/plans")
    public R<List<SubscriptionPlan>> getPlans() {
        return R.ok(subscriptionService.getAllPlans());
    }

    /**
     * 获取当前订阅
     */
    @GetMapping("/current")
    public R<UserSubscription> getCurrentSubscription(@RequestAttribute("currentUserId") Long userId) {
        return R.ok(subscriptionService.getCurrentSubscription(userId));
    }

    /**
     * 获取用户当前额度状态
     */
    @GetMapping("/status")
    public R<SubscriptionStatusDTO> getSubscriptionStatus(@RequestAttribute("currentUserId") Long userId) {
        return R.ok(subscriptionService.getSubscriptionStatus(userId));
    }
}
