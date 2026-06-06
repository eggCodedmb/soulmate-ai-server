package com.soulmate.service;

import com.soulmate.domain.dto.PaymentCreateResponse;
import com.soulmate.domain.entity.PaymentOrder;

import java.util.Map;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 创建支付订单并获取支付宝支付表单
     *
     * @param userId 用户ID
     * @param planId 套餐ID
     * @return 支付响应（包含订单号和支付表单）
     */
    PaymentCreateResponse createPaymentOrder(Long userId, Long planId);

    /**
     * 处理支付宝异步通知（含验签）
     *
     * @param params 支付宝回调参数
     * @return 处理结果字符串（"success" 或 "fail"）
     */
    String handleAlipayNotify(Map<String, String> params);

    /**
     * 处理支付回调（内部调用）
     */
    void handlePaymentCallback(String orderNo, String tradeNo);

    /**
     * 查询订单状态
     */
    PaymentOrder getOrderStatus(String orderNo);
}
