package com.soulmate.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建支付响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateResponse {

    /** 订单号 */
    private String orderNo;

    /** 支付表单 HTML（前端渲染后自动跳转支付宝） */
    private String payForm;
}
