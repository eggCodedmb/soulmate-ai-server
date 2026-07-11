package com.soulmate.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soulmate.common.config.AlipayProperties;
import com.soulmate.common.exception.BizException;
import com.soulmate.common.response.ResultCode;
import com.soulmate.domain.dto.PaymentCreateResponse;
import com.soulmate.domain.entity.PaymentOrder;
import com.soulmate.domain.entity.SubscriptionPlan;
import com.soulmate.domain.enums.PaymentStatus;
import com.soulmate.mapper.PaymentOrderMapper;
import com.soulmate.mapper.SubscriptionPlanMapper;
import com.soulmate.service.PaymentService;
import com.soulmate.service.SubscriptionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付服务实现 - 支付宝沙箱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final SubscriptionPlanMapper planMapper;
    private final SubscriptionService subscriptionService;
    private final AlipayProperties alipayProperties;

    private AlipayClient alipayClient;

    @PostConstruct
    public void init() {
        this.alipayClient = new DefaultAlipayClient(
                alipayProperties.getGateway(),
                alipayProperties.getAppId(),
                alipayProperties.getPrivateKey(),
                alipayProperties.getFormat(),
                alipayProperties.getCharset(),
                alipayProperties.getAlipayPublicKey(),
                alipayProperties.getSignType()
        );
    }

    @Override
    @Transactional
    public PaymentCreateResponse createPaymentOrder(Long userId, Long planId) {
        // 查询套餐
        SubscriptionPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ResultCode.PLAN_NOT_FOUND);
        }

        if (plan.getPriceMonthly().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("免费套餐无需支付");
        }

        // 校验是否降级：目标套餐级别不能低于当前套餐
        SubscriptionPlan currentPlan = subscriptionService.getUserPlan(userId);
        if (currentPlan != null && plan.getDisplayOrder() <= currentPlan.getDisplayOrder()) {
            throw new BizException(ResultCode.SUBSCRIPTION_DOWNGRADE_NOT_ALLOWED);
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

        // 构建支付宝 App 支付签名串
        String payForm = doAlipayTradeAppPay(order, plan);

        return PaymentCreateResponse.builder()
                .orderNo(order.getOrderNo())
                .payForm(payForm)
                .build();
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
    public String handleAlipayNotify(Map<String, String> params) {
        try {
            // 验签
            boolean verified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            );

            if (!verified) {
                log.warn("支付宝异步通知验签失败");
                return "fail";
            }

            String orderNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");

            log.info("支付宝异步通知: orderNo={}, tradeNo={}, tradeStatus={}", orderNo, tradeNo, tradeStatus);

            // 交易成功或交易完成都视为支付成功
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                PaymentOrder order = getOrderStatus(orderNo);
                if (order != null && order.getPaymentStatus() == PaymentStatus.PENDING) {
                    handlePaymentCallback(orderNo, tradeNo);
                } else {
                    log.info("订单已处理或不存在，跳过: orderNo={}", orderNo);
                }
            }

            return "success";
        } catch (AlipayApiException e) {
            log.error("支付宝异步通知处理异常", e);
            return "fail";
        }
    }

    @Override
    @Transactional
    public PaymentOrder getOrderStatus(String orderNo) {
        PaymentOrder order = paymentOrderMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>()
                        .eq(PaymentOrder::getOrderNo, orderNo));

        if (order != null && order.getPaymentStatus() == PaymentStatus.PENDING) {
            // 主动向支付宝查询订单状态，防止在本地开发测试（如 localhost 无公网IP）时，支付宝异步回调无法送达导致状态不更新
            try {
                AlipayTradeQueryRequest queryRequest = new AlipayTradeQueryRequest();
                JSONObject bizContent = new JSONObject();
                bizContent.put("out_trade_no", orderNo);
                queryRequest.setBizContent(bizContent.toString());

                AlipayTradeQueryResponse queryResponse = alipayClient.execute(queryRequest);
                if (queryResponse.isSuccess()) {
                    String tradeStatus = queryResponse.getTradeStatus();
                    if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                        log.info("主动查询支付宝订单成功，订单已支付: orderNo={}, tradeNo={}", orderNo, queryResponse.getTradeNo());
                        handlePaymentCallback(orderNo, queryResponse.getTradeNo());
                        // 重新获取已更新的订单状态
                        order = paymentOrderMapper.selectById(order.getId());
                    }
                }
            } catch (AlipayApiException e) {
                log.error("主动查询支付宝订单异常: orderNo={}", orderNo, e);
            }
        }

        return order;
    }

    /**
     * 调用支付宝 APP 支付接口，生成签名订单字符串
     */
    private String doAlipayTradeAppPay(PaymentOrder order, SubscriptionPlan plan) {
        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        request.setNotifyUrl(alipayProperties.getNotifyUrl());

        // 业务参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("total_amount", order.getAmount().toPlainString());
        bizContent.put("subject", "SoulMate AI - " + plan.getPlanName() + " 套餐");
        bizContent.put("product_code", "QUICK_MSECURITY_PAY");
        request.setBizContent(bizContent.toString());

        try {
            // 使用 sdkExecute 得到签名订单串，而非 pageExecute 得到 HTML 表单
            String orderString = alipayClient.sdkExecute(request).getBody();
            log.info("支付宝 APP 支付签名串生成成功: orderNo={}", order.getOrderNo());
            return orderString;
        } catch (AlipayApiException e) {
            log.error("支付宝 APP 支付签名串生成失败: orderNo={}", order.getOrderNo(), e);
            throw new BizException(ResultCode.PAYMENT_FAILED);
        }
    }

    /**
     * 生成订单号：时间戳 + 随机串
     */
    private String generateOrderNo() {
        return "SMA" + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase();
    }
}
