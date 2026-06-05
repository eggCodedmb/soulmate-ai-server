package com.soulmate.service;

import com.soulmate.domain.entity.PaymentOrder;

/**
 * 支付服务
 */
public interface PaymentService {

    /**
     * 创建支付订单
     */
    PaymentOrder createPaymentOrder(Long userId, Long planId);

    /**
     * 处理支付回调
     */
    void handlePaymentCallback(String orderNo, String tradeNo);

    /**
     * 查询订单状态
     */
    PaymentOrder getOrderStatus(String orderNo);
}
