package com.soulmate.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soulmate.common.exception.BizException;
import com.soulmate.common.response.ResultCode;
import com.soulmate.domain.entity.PaymentOrder;
import com.soulmate.domain.entity.SubscriptionPlan;
import com.soulmate.domain.enums.PaymentStatus;
import com.soulmate.mapper.PaymentOrderMapper;
import com.soulmate.mapper.SubscriptionPlanMapper;
import com.soulmate.service.PaymentService;
import com.soulmate.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final SubscriptionPlanMapper planMapper;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional
    public PaymentOrder createPaymentOrder(Long userId, Long planId) {
        // 查询套餐
        SubscriptionPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ResultCode.PLAN_NOT_FOUND);
        }

        if (plan.getPriceMonthly().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("免费套餐无需支付");
        }

        // 创建支付订单
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPlanId(planId);
        order.setAmount(plan.getPriceMonthly());
        order.setPaymentChannel("alipay");
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPeriodType("monthly");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        paymentOrderMapper.insert(order);

        log.info("支付订单创建成功: orderNo={}, userId={}, planId={}, amount={}",
                order.getOrderNo(), userId, planId, order.getAmount());

        return order;
    }

    @Override
    @Transactional
    public void handlePaymentCallback(String orderNo, String tradeNo) {
        PaymentOrder order = getOrderStatus(orderNo);
        if (order == null) {
            log.warn("支付回调：订单不存在, orderNo={}", orderNo);
            return;
        }

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            log.warn("支付回调：订单状态异常, orderNo={}, status={}", orderNo, order.getPaymentStatus());
            return;
        }

        // 更新订单状态
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setTradeNo(tradeNo);
        order.setPaidTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        paymentOrderMapper.updateById(order);

        // 激活订阅
        subscriptionService.activateSubscription(order.getUserId(), order.getPlanId());

        log.info("支付回调处理成功: orderNo={}, tradeNo={}", orderNo, tradeNo);
    }

    @Override
    public PaymentOrder getOrderStatus(String orderNo) {
        return paymentOrderMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>()
                        .eq(PaymentOrder::getOrderNo, orderNo));
    }

    /**
     * 生成订单号：时间戳 + 随机串
     */
    private String generateOrderNo() {
        return "SMA" + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase();
    }
}
