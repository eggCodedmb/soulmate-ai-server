package com.soulmate.web.controller;

import com.soulmate.common.response.R;
import com.soulmate.domain.dto.PaymentCreateResponse;
import com.soulmate.domain.entity.PaymentOrder;
import com.soulmate.service.PaymentService;
import com.soulmate.web.dto.CreatePaymentRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 创建支付订单，返回支付宝支付表单
     */
    @PostMapping("/create")
    public R<PaymentCreateResponse> createPayment(
            @RequestAttribute("currentUserId") Long userId,
            @Valid @RequestBody CreatePaymentRequest request) {
        PaymentCreateResponse response = paymentService.createPaymentOrder(userId, request.getPlanId());
        return R.ok(response);
    }

    /**
     * 支付宝异步通知回调（无需鉴权，由支付宝服务器调用）
     */
    @PostMapping("/notify")
    public String handleNotify(HttpServletRequest request) {
        // 提取支付宝回调参数
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String[] values = entry.getValue();
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append(values[i]);
                if (i < values.length - 1) {
                    valueStr.append(",");
                }
            }
            params.put(entry.getKey(), valueStr.toString());
        }

        log.info("收到支付宝异步通知: orderNo={}", params.get("out_trade_no"));
        return paymentService.handleAlipayNotify(params);
    }

    /**
     * 查询支付订单状态
     */
    @GetMapping("/status")
    public R<PaymentOrder> getOrderStatus(@RequestParam String orderNo) {
        PaymentOrder order = paymentService.getOrderStatus(orderNo);
        return R.ok(order);
    }
}
